module Main exposing (main)

import Browser
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Person exposing (..)
import Http exposing (..)
import Maybe exposing (..)

main : Program () Model Msg
main = Browser.element { init = init, update = update, subscriptions = subscriptions, view = view }

type alias Data = 
  { persons    : List  Person
  , viewPerson : Maybe Person
  , firstName  : String
  , lastName   : String
  , nickName   : String
  }

type Model = Loading | Failure | Display Data

init : () -> (Model, Cmd Msg)
init _ = (Loading, loadPersons)

type Msg
  = LoadPersons  (Result Error (List Person))
  | LoadPerson   (Result Error Person)
  | Reloaded
  | ShowPerson   String
  | DeletePerson String
  | AddPerson    Person
  | ChangeFN     String
  | ChangeLN     String
  | ChangeNN     String

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

changeField : Model -> Maybe String -> Maybe String -> Maybe String -> (Model, Cmd Msg)
changeField model firstName lastName nickName = 
      case model of
        Display data -> ((Display (Data data.persons null (or data.firstName firstName) (or data.lastName lastName) (or data.nickName nickName))), stop)
        _            -> (model, stop)

update : Msg -> Model -> (Model, Cmd Msg)
update msg model =
  case msg of
    LoadPersons result ->
      case result of
        Ok persons -> ((Display (Data persons null "" "" "")), stop)
        Err _      -> (Failure, stop)

    LoadPerson result ->
      case result of
        Ok person ->
          case model of
            Display data -> ((Display (Data data.persons (Just person) "" "" "")), stop)
            Loading      -> ((Display (Data []           null          "" "" "")), stop)
            Failure      -> (Failure, stop)
        Err _            -> (Failure, stop)
    
    Reloaded               -> (model, loadPersons)
    ShowPerson   personId  -> (model, loadPerson   personId)
    DeletePerson personId  -> (model, deletePerson personId)
    AddPerson    person    -> (model, addPerson    person)
    ChangeFN     firstName -> (changeField model (Just firstName)  null            null)
    ChangeLN     lastName  -> (changeField model  null            (Just lastName)  null)
    ChangeNN     nickName  -> (changeField model  null             null           (Just nickName))


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
          [ h2  [] [ text "Persons" ]
          , div [ class "persons" ] [ ul [] (data.persons |> List.map (\person -> viewEachPerson person)) ]
          
          , h2  [] [ text "New Person" ]
          , div [ class "persons" ]
                [ div [] [ span   [ class "person-first"] [ text "First name: ", input [ placeholder "First name", value data.firstName, onInput ChangeFN ] []]]
                , div [] [ span   [ class "person-last"]  [ text "Last name: ",  input [ placeholder "Last name",  value data.lastName,  onInput ChangeLN ] []]]
                , div [] [ span   [ class "person-nick"]  [ text "Nick name: ",  input [ placeholder "Nick name",  value data.nickName,  onInput ChangeNN ] []]]
                , div [] [ button [ onClick (AddPerson (Person null data.firstName data.lastName (Just data.nickName))) ] [ text "Add" ]]
                ]
          
          , or (div [][]) (Maybe.map viewPerson data.viewPerson)
          ]

viewEachPerson : Person -> Html Msg
viewEachPerson person = 
  div [ class "person" ]
      [ span [ class "remove-person", onClick (DeletePerson (or "-" person.id))] [ text " x " ]
      , span [ class "item-person",   onClick (ShowPerson   (or "-" person.id))] [ text (person.firstName ++ " " ++ person.lastName ++ (wrap person.nickName)) ]
      ]

viewPerson : Person -> Html Msg
viewPerson person = 
  div [ class "person" ]
      [ div [ class "person-id" ]    [ span [][text "ID"],         text (or "-" person.id)]
      , div [ class "person-first" ] [ span [][text "First name"], text person.firstName ]
      , div [ class "person-last" ]  [ span [][text "Last name"],  text person.lastName  ]
      , div [ class "person-nick" ]  [ span [][text "Nick name"],  text (or "<no-nick-name>" person.nickName) ]
      ]


-- HTTP

loadPersons : (Cmd Msg)
loadPersons = get { url = "/api/persons", expect = expectJson LoadPersons personListDecoder }

loadPerson : String -> (Cmd Msg)
loadPerson id = get { url = "/api/persons/" ++ id, expect = expectJson LoadPerson personDecoder }

addPerson : Person -> (Cmd Msg)
addPerson person = post { url = "/api/persons/", body = jsonBody (personEncoder person), expect = expectWhatever (\_ -> Reloaded) }

deletePerson : String -> (Cmd Msg)
deletePerson id = delete { url = "/api/persons/" ++ id, expect = expectWhatever (\_ -> Reloaded) }

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
