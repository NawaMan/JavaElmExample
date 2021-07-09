module Main exposing (main)

import Browser
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Http
import Person exposing (..)


main =
  Browser.element
    { init          = init
    , update        = update
    , subscriptions = subscriptions
    , view          = view
    }


type Model
  = Loading
  | Failure
  | Success (List Person)
  | View    ((List Person), Person)


init : () -> (Model, Cmd Msg)
init _ =
  (Loading, loadPersons)


type Msg
  = LoadPersons (Result Http.Error (List Person))
  | LoadPerson  (Result Http.Error Person)
  | ShowPerson  String


update : Msg -> Model -> (Model, Cmd Msg)
update msg model =
  case msg of
    LoadPersons result ->
      case result of
        Ok persons ->
          (Success persons, Cmd.none)

        Err _ ->
          (Failure, Cmd.none)

    LoadPerson result ->
      case result of
        Ok person ->
          case model of
            Loading ->
              (View ([], person), Cmd.none)
            Failure ->
              (View ([], person), Cmd.none)
            Success persons ->
              (View (persons, person), Cmd.none)
            View (persons, _) ->
              (View (persons, person), Cmd.none)

        Err _ ->
          (Failure, Cmd.none)
    ShowPerson id ->
      (model, loadPerson id)



-- SUBSCRIPTIONS


subscriptions : Model -> Sub Msg
subscriptions model =
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

    Success persons ->
      div []
          [ h2 [] [ text "Persons" ]
          , div [ class "persons" ]
            [ ul []
              (persons
                |> List.map (\person -> viewEachPerson person)
              )
            ]
          ]
    View (persons, person) -> 
      viewPerson persons person


viewEachPerson : Person -> Html Msg
viewEachPerson person = 
  div 
    [ class "person"
    , onClick (ShowPerson (Maybe.withDefault "-" person.id)) ]
    [ span [class "remove-person"] 
           [text " x "]
    , text person.firstName
    , text " "
    , text person.lastName
    ]


viewPerson : (List Person) -> Person -> Html Msg
viewPerson persons person = 
  div [ class "person" ]
  [ div []
    [ h2 [] [ text "Persons" ]
    , div [ class "persons" ]
      [ ul []
        (persons
          |> List.map (\p -> viewEachPerson p)
        )
      ]
    ]
  , div [ class "person-field" ]
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
