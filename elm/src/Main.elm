module Main exposing (main)

import Browser
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Person exposing (..)
import Http exposing (..)
import Maybe exposing (..)

main : Program () Model Msg
main = Browser.element { 
    init          = init, 
    update        = update, 
    subscriptions = subscriptions, 
    view          = view
  }

type Mode = Listing | Adding | Viewing | Editing

type alias Data = 
  { persons : List Person
  , person  : Person
  , mode    : Mode
  }

type Model = Loading | Failure | Display Data

type Msg
  = Reloaded
  | LoadPersons (Result Error (List Person))
  | LoadPerson  (Result Error Person)
  | ViewPerson   String
  | AddPerson    Person
  | ChangePerson Person
  | DeletePerson String
  -- Change mode
  | ToListing Data
  | ToViewing Data
  | ToAdding  Data
  | ToEditing Data
  -- Editing fields
  | EditFirstName String
  | EditLastName  String
  | EditNickName  String

emptyPerson : Person
emptyPerson = Person Nothing "" "" Nothing

init : () -> (Model, Cmd Msg)
init _ = (Loading, loadPersons)


changeField : Model -> Maybe String -> Maybe String -> Maybe String -> (Model, Cmd Msg)
changeField model firstName lastName nickName = 
      case model of
        Display data -> 
              ((Display 
                (Data 
                  data.persons 
                  (Person 
                    data.person.id 
                    (firstName |> withDefault data.person.firstName) 
                    (lastName  |> withDefault data.person.lastName) 
                    (Just (nickName |> withDefault (data.person.nickName |> withDefault "")))
                  )
                  data.mode
                )
              ),
              Cmd.none)
        _ -> (model, Cmd.none)

update : Msg -> Model -> (Model, Cmd Msg)
update msg model =
  case msg of
    Reloaded -> (model, loadPersons)

    LoadPersons result ->
      case result of
        Ok persons -> ((Display (Data persons emptyPerson Listing)), Cmd.none)
        Err _      -> (Failure, Cmd.none)

    LoadPerson result ->
      case result of
        Ok person ->
          case model of
            Display data -> ((Display (Data data.persons person      Viewing)), Cmd.none)
            Loading      -> ((Display (Data []           emptyPerson Listing)), Cmd.none)
            Failure      -> (Failure, Cmd.none)
        Err _            -> (Failure, Cmd.none)
    
    AddPerson    person   -> (model, addPerson    person)
    ViewPerson   personId -> (model, loadPerson   personId)
    ChangePerson person   -> (model, changePerson person)
    DeletePerson personId -> (model, deletePerson personId)

    ToListing data -> (Display (Data data.persons emptyPerson Listing), Cmd.none)
    ToViewing data -> (Display (Data data.persons data.person Viewing), Cmd.none)
    ToAdding  data -> (Display (Data data.persons emptyPerson Adding),  Cmd.none)
    ToEditing data -> (Display (Data data.persons data.person Editing), Cmd.none)

    EditFirstName firstName -> (changeField model (Just firstName)  Nothing         Nothing)
    EditLastName  lastName  -> (changeField model  Nothing         (Just lastName)  Nothing)
    EditNickName  nickName  -> (changeField model  Nothing          Nothing        (Just nickName))


-- SUBSCRIPTIONS

subscriptions : Model -> Sub Msg
subscriptions _ = Sub.none


-- VIEW

view : Model -> Html Msg
view model = 
  case model of
    Failure -> div [] [ text "I could not load person information. " ]
    Loading -> text "Loading..."
    Display data ->
      div []
          [ data |> listPersons
          , case data.mode of
              Listing -> div [][]
              Adding  -> data |> newPerson
              Viewing -> data |> viewPerson
              Editing -> data |> editPerson
          ]

listPersons : Data -> Html Msg
listPersons data = 
  div []
      [ h2     []                          [ text "Persons" ]
      , div    []                          [ ul [] (data.persons |> List.map (\person -> viewEachPerson person)) ]
      , button [ onClick (ToAdding data) ] [ text "New person" ]
      ]

viewEachPerson : Person -> Html Msg
viewEachPerson person = 
  div []
      [ span [ onClick (DeletePerson (person.id |> withDefault "-"))] 
             [ text " x " ]
      , span [ onClick (ViewPerson   (person.id |> withDefault "-"))] 
             [ text (person.firstName ++ " " ++ person.lastName ++ (wrap person.nickName)) ]
      ]

viewPerson : Data -> Html Msg
viewPerson data = 
  let person = data.person
      personId = person.id |> withDefault "-"
      personNickName = person.nickName |> withDefault ""
  in  div []
      [ h2  [] [ text "Person" ]
      , div [] [ span [] [text "ID"],         text personId]
      , div [] [ span [] [text "First name"], text person.firstName ]
      , div [] [ span [] [text "Last name"],  text person.lastName  ]
      , div [] [ span [] [text "Nick name"],  text personNickName ]
      , div [] [ button [ onClick (ToEditing    data)     ] [ text "Edit" ]
               , button [ onClick (DeletePerson personId) ] [ text "Delete" ]
               , button [ onClick (ToListing    data)     ] [ text "Cancel" ]
               ]
      ]

editPerson : Data -> Html Msg
editPerson data =
  let person = data.person
      personId = person.id |> withDefault "-"
      personNickName = person.nickName |> withDefault ""
  in  div []
      [ h2  [] [ text "Edit Person" ]
      , div [] [ span [] [text "ID"],         text personId]
      , div [] [ span [] [text "First name"], input [ value person.firstName, onInput EditFirstName ] []]
      , div [] [ span [] [text "Last name"],  input [ value person.lastName,  onInput EditLastName  ] []]
      , div [] [ span [] [text "Nick name"],  input [ value personNickName,   onInput EditNickName  ] []]
      , div [] [ button [ onClick (ChangePerson person) ] [ text "Save" ]
               , button [ onClick (ToViewing    data)   ] [ text "Cancel" ]
               ]
      ]

newPerson : Data -> Html Msg
newPerson data =
  let person = data.person
      personNickName = person.nickName |> withDefault ""
  in  div []
      [ h2  [] [ text "New Person" ]
      , div [] [ span [] [text "First name"], input [ value person.firstName, onInput EditFirstName ] []]
      , div [] [ span [] [text "Last name"],  input [ value person.lastName,  onInput EditLastName  ] []]
      , div [] [ span [] [text "Nick name"],  input [ value personNickName,   onInput EditNickName  ] []]
      , div [] [ button [ onClick (AddPerson data.person) ] [ text "Add" ]
               , button [ onClick (ToListing data)        ] [ text "Cancel" ]
               ]
      ]


-- HTTP

loadPersons : (Cmd Msg)
loadPersons = get { 
    url    = "/api/persons", 
    expect = expectJson LoadPersons personListDecoder
  }

loadPerson : String -> (Cmd Msg)
loadPerson id = get {
    url    = "/api/persons/" ++ id,
    expect = expectJson LoadPerson personDecoder
  }

addPerson : Person -> (Cmd Msg)
addPerson person = post {
    url    = "/api/persons/",
    body   = jsonBody (personEncoder (Person Nothing person.firstName person.lastName person.nickName)),
    expect = expectWhatever (\_ -> Reloaded)
  }

deletePerson : String -> (Cmd Msg)
deletePerson id = delete { 
    url    = "/api/persons/" ++ id, 
    expect = expectWhatever (\_ -> Reloaded)  -- Reloaded or Show error 
  }

changePerson : Person -> (Cmd Msg)
changePerson person = 
    case person.id of
      Nothing -> Cmd.none
      Just id -> change {
          url    = "/api/persons/" ++ id,
          body   = jsonBody (personEncoder person),
          expect = expectWhatever (\_ -> Reloaded)  -- Reloaded or Show error 
        }


-- utility functions --

-- stop : Cmd msg
-- stop = Cmd.none

wrap : Maybe String -> String
wrap text =
  case text of
    Just t  -> " (" ++ t ++ ")"
    Nothing -> ""

delete : { url: String, expect: Http.Expect Msg } -> (Cmd Msg)
delete spec =
  Http.request
    { method = "DELETE"
    , headers = []
    , url = spec.url
    , body = emptyBody
    , expect = spec.expect
    , timeout = Nothing
    , tracker = Nothing
    }

change : { url: String, body: Body, expect: Http.Expect Msg } -> (Cmd Msg)
change spec =
  Http.request
    { method = "PUT"
    , headers = []
    , url = spec.url
    , body = spec.body
    , expect = spec.expect
    , timeout = Nothing
    , tracker = Nothing
    }