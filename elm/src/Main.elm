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
emptyPerson = Person null "" "" null

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
                    (firstName |> or data.person.firstName) 
                    (lastName  |> or data.person.lastName) 
                    (Just (nickName |> or (data.person.nickName |> or "")))
                  )
                  data.mode
                )
              ),
              stop)
        _ ->  (model, stop)

update : Msg -> Model -> (Model, Cmd Msg)
update msg model =
  case msg of
    Reloaded -> (model, loadPersons)

    LoadPersons result ->
      case result of
        Ok persons -> ((Display (Data persons emptyPerson Listing)), stop)
        Err _      -> (Failure, stop)

    LoadPerson result ->
      case result of
        Ok person ->
          case model of
            Display data -> ((Display (Data data.persons person      Viewing)), stop)
            Loading      -> ((Display (Data []           emptyPerson Listing)), stop)
            Failure      -> (Failure, stop)
        Err _            -> (Failure, stop)
    
    AddPerson    person   -> (model, addPerson    person)
    ViewPerson   personId -> (model, loadPerson   personId)
    ChangePerson person   -> (model, changePerson person)
    DeletePerson personId -> (model, deletePerson personId)

    ToListing data -> (Display (Data data.persons emptyPerson Listing), stop)
    ToViewing data -> (Display (Data data.persons data.person Viewing), stop)
    ToAdding  data -> (Display (Data data.persons emptyPerson Adding),  stop)
    ToEditing data -> (Display (Data data.persons data.person Editing), stop)

    EditFirstName firstName -> (changeField model (Just firstName)  null            null)
    EditLastName  lastName  -> (changeField model  null            (Just lastName)  null)
    EditNickName  nickName  -> (changeField model  null             null           (Just nickName))


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
      [ h2  [] [ text "Persons" ]
      , div [ class "persons" ] [ ul [] (data.persons |> List.map (\person -> viewEachPerson person)) ]
      , button [ onClick (ToAdding data) ] [ text "New person" ]
      ]

viewEachPerson : Person -> Html Msg
viewEachPerson person = 
  div [ class "person" ]
      [ span [ class "remove", onClick (DeletePerson (person.id |> or "-"))] [ text " x " ]
      , span [ class "item",   onClick (ViewPerson   (person.id |> or "-"))] [ text (person.firstName ++ " " ++ person.lastName ++ (wrap person.nickName)) ]
      ]

viewPerson : Data -> Html Msg
viewPerson data = 
  div [ class "person" ]
      [ h2  [] [ text "Person" ]
      , div [ class "person-id" ]    [ span [] [text "ID"],         text (data.person.id |> or "-")]
      , div [ class "person-first" ] [ span [] [text "First name"], text  data.person.firstName ]
      , div [ class "person-last" ]  [ span [] [text "Last name"],  text  data.person.lastName  ]
      , div [ class "person-nick" ]  [ span [] [text "Nick name"],  text (data.person.nickName |> or "<no-nick-name>") ]
      , div []
            [ button [ onClick (ToEditing     data)                      ] [ text "Edit" ]
            , button [ onClick (DeletePerson (data.person.id |> or "-")) ] [ text "Delete" ]
            , button [ onClick (ToListing     data)                      ] [ text "Cancel" ]
            ]
      ]

editPerson : Data -> Html Msg
editPerson data =
  div [ class "person" ]
      [ h2  [] [ text "Edit Person" ]
      , div [] [ span   [ class "id"    ] [ text "ID",         text (data.person.id |> or "-")]]
      , div [] [ span   [ class "first" ] [ text "First name", input [ placeholder "First name", value  data.person.firstName,          onInput EditFirstName ] []]]
      , div [] [ span   [ class "last"  ] [ text "Last name",  input [ placeholder "Last name",  value  data.person.lastName,           onInput EditLastName ] []]]
      , div [] [ span   [ class "nick"  ] [ text "Nick name",  input [ placeholder "Nick name",  value (data.person.nickName |> or ""), onInput EditNickName ] []]]
      , div [] [ button [ onClick (ChangePerson data.person) ] [ text "Save" ]
               , button [ onClick (ToViewing    data)        ] [ text "Cancel" ]
               ]
      ]

newPerson : Data -> Html Msg
newPerson data =
  div [ class "person" ]
      [ h2  [] [ text "New Person" ]
      , div [] [ span   [ class "first" ] [ text "First name", input [ placeholder "First name", value  data.person.firstName,          onInput EditFirstName ] []]]
      , div [] [ span   [ class "last"  ] [ text "Last name",  input [ placeholder "Last name",  value  data.person.lastName,           onInput EditLastName ] []]]
      , div [] [ span   [ class "nick"  ] [ text "Nick name",  input [ placeholder "Nick name",  value (data.person.nickName |> or ""), onInput EditNickName ] []]]
      , div [] [ button [ onClick (AddPerson (Person null data.person.firstName data.person.lastName data.person.nickName)) ] [ text "Add" ]
               , button [ onClick (ToListing data)                                                                          ] [ text "Cancel" ]
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
    body   = jsonBody (personEncoder person),
    expect = expectWhatever (\_ -> Reloaded)
  }

deletePerson : String -> (Cmd Msg)
deletePerson id = delete { 
    url    = "/api/persons/" ++ id, 
    expect = expectWhatever (\_ -> Reloaded)
  }

changePerson : Person -> (Cmd Msg)
changePerson person = 
    case person.id of
      Nothing -> stop
      Just id -> change {
          url    = "/api/persons/" ++ id,
          body   = jsonBody (personEncoder person),
          expect = expectWhatever (\_ -> Reloaded)
        }


-- utility functions --

or : a -> Maybe a -> a
or = withDefault

null : Maybe a
null = Nothing

stop : Cmd msg
stop = Cmd.none

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
    , timeout = null
    , tracker = null
    }

change : { url: String, body: Body, expect: Http.Expect Msg } -> (Cmd Msg)
change spec =
  Http.request
    { method = "PUT"
    , headers = []
    , url = spec.url
    , body = spec.body
    , expect = spec.expect
    , timeout = null
    , tracker = null
    }