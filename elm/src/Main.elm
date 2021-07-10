module Main exposing (main)

import Browser
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Http
import Person exposing (..)

main : Program () Model Msg
main =
  Browser.element
    { init          = init
    , update        = update
    , subscriptions = subscriptions
    , view          = view
    }


type alias Data = 
  { persons            : List  Person
  , viewPerson         : Maybe Person
  , newPersonFirstName : String
  , newPersonLastName  : String
  , newPersonNickName  : String
  }


type Model
  = Loading
  | Failure
  | Display Data


init : () -> (Model, Cmd Msg)
init _ =
  (Loading, loadPersons)


type Msg
  = LoadPersons  (Result Http.Error (List Person))
  | LoadPerson   (Result Http.Error Person)
  | ShowPerson   String
  | DeletePerson String
  | Deleted
  | Added
  | AddPerson    Person
  | ChangeFN     String
  | ChangeLN     String
  | ChangeNN     String


update : Msg -> Model -> (Model, Cmd Msg)
update msg model =
  case msg of
    LoadPersons result ->
      case result of
        Ok persons ->
          ((Display (Data persons Maybe.Nothing "" "" "")), Cmd.none)

        Err _ ->
          (Failure, Cmd.none)

    LoadPerson result ->
      case result of
        Ok person ->
          case model of
            Display data ->
              ((Display (Data data.persons (Maybe.Just person) "" "" "")), Cmd.none)
            _ ->
              ((Display (Data [] Maybe.Nothing "" "" "")), Cmd.none)

        Err _ ->
          (Failure, Cmd.none)
    ShowPerson id ->
      (model, loadPerson id)
    DeletePerson id ->
      (model, deletePerson id)
    Deleted ->
      (model, loadPersons)
    Added ->
      (model, loadPersons)
    AddPerson person ->
      (model, addPerson person)
    ChangeFN firstName ->
      case model of
        Display data ->
          ((Display (Data data.persons Maybe.Nothing firstName data.newPersonLastName data.newPersonNickName)), Cmd.none)
        _ ->
          (model, Cmd.none)
    ChangeLN lastName ->
      case model of
        Display data ->
          ((Display (Data data.persons Maybe.Nothing data.newPersonFirstName lastName data.newPersonNickName)), Cmd.none)
        _ ->
          (model, Cmd.none)
    ChangeNN nickName ->
      case model of
        Display data ->
          ((Display (Data data.persons Maybe.Nothing data.newPersonFirstName data.newPersonLastName nickName)), Cmd.none)
        _ ->
          (model, Cmd.none)


-- SUBSCRIPTIONS


subscriptions : Model -> Sub Msg
subscriptions _ =
  Sub.none


-- VIEW


view : Model -> Html Msg
view model =
    viewPersons model


viewPersons : Model -> Html Msg
viewPersons model =
  case model of
    Failure ->
      div []
        [ text "I could not load `person.json`. " ]

    Loading ->
      text "Loading..."

    Display data ->
      div []
          [ h2 [] [ text "Persons" ]
          , div [ class "persons" ]
                [ ul []
                  (data.persons
                    |> List.map (\person -> viewEachPerson person)
                  )
                ]
          , h2 [] [ text "New Person" ]
          , div [ class "persons" ]
              [ div []
                    [ span [class "person-label"] 
                           [ text "First name: "
                           , input [ placeholder "First name", value data.newPersonFirstName, onInput ChangeFN ] []
                           ]
                    ]
              , div []
                    [ span [class "person-label"] 
                           [ text "Last name: "
                           , input [ placeholder "Last name", value data.newPersonLastName, onInput ChangeLN ] []
                           ]
                    ]
              , div []
                    [ span [class "person-label"] 
                           [ text "Nick name: "
                           , input [ placeholder "Nick name", value data.newPersonNickName, onInput ChangeNN ] []
                           ]
                    ]
              , div []
                    [ button [ onClick (AddPerson (Person Maybe.Nothing data.newPersonFirstName data.newPersonLastName (Maybe.Just data.newPersonNickName))) ]
                             [ text "Add" ]
                    ]
              ]
          , case data.viewPerson of 
                Just person ->
                  viewPerson person

                Maybe.Nothing ->
                  div [][]
          ]


viewEachPerson : Person -> Html Msg
viewEachPerson person = 
  div []
      [ div 
        [ class "person" ]
        [ span [ class "remove-person"
               , onClick (DeletePerson (Maybe.withDefault "-" person.id))
               ] 
               [text " x "]
        , span [ onClick (ShowPerson (Maybe.withDefault "-" person.id))]
               [ text person.firstName
               , text " "
               , text person.lastName
               ]
        ]
      ]


viewPerson : Person -> Html Msg
viewPerson person = 
  div [ class "person" ]
  [ div [ class "person-field" ]
      [ span [class "person-label"] 
             [text "ID"]
      , text (Maybe.withDefault "-" person.id)
      ]
    , div [ class "person-field" ]
      [ span [class "person-label"] 
             [text "First name"]
      , text person.firstName
      ]
  , div [ class "person-field" ]
      [ span [class "person-label"] 
             [text "Last name"]
      , text person.lastName
      ]
  , div [ class "person-field" ]
      [ span [class "person-label"] 
             [text "Nick name"]
      , text (Maybe.withDefault "<no-nick-name>" person.nickName)
      ]
  ]


-- HTTP


loadPersons : (Cmd Msg)
loadPersons =
  Http.get
    { url = "/api/persons"
    , expect = Http.expectJson LoadPersons personListDecoder
    }


loadPerson : String -> (Cmd Msg)
loadPerson id =
  Http.get
    { url = "/api/persons/" ++ id
    , expect = Http.expectJson LoadPerson personDecoder
    }


deletePerson : String -> (Cmd Msg)
deletePerson id =
  Http.request
    { method = "DELETE"
    , headers = []
    , url = "/api/persons/" ++ id
    , body = Http.emptyBody
    , expect = Http.expectWhatever (\_ -> Deleted)
    , timeout = Nothing
    , tracker = Nothing
    }


addPerson : Person -> (Cmd Msg)
addPerson person =
  Http.post
    { url = "/api/persons/"
    , body = Http.jsonBody (personEncoder person)
    , expect = Http.expectWhatever (\_ -> Added)
    }
