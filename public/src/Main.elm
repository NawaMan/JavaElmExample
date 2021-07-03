module Main exposing (main)

import Browser
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Http
import Person exposing (..)


main =
  Browser.element
    { init = init
    , update = update
    , subscriptions = subscriptions
    , view = view
    }


type Model
  = Loading
  | Failure
  | Success Person


init : () -> (Model, Cmd Msg)
init _ =
  (Loading, loadPersons)


type Msg
  = LoadPerson (Result Http.Error Person)


update : Msg -> Model -> (Model, Cmd Msg)
update msg model =
  case msg of
    LoadPerson result ->
      case result of
        Ok url ->
          (Success url, Cmd.none)

        Err _ ->
          (Failure, Cmd.none)



-- SUBSCRIPTIONS


subscriptions : Model -> Sub Msg
subscriptions model =
  Sub.none


-- VIEW


view : Model -> Html Msg
view model =
  div []
    [ h2 [] [ text "Persons" ]
    , viewPerson model
    ]


viewPerson : Model -> Html Msg
viewPerson model =
  case model of
    Failure ->
      div []
        [ text "I could not load `person.json`. " ]

    Loading ->
      text "Loading..."

    Success person ->
      div []
        [ text "I got a person: "
        , text person.firstName
        ]



-- HTTP


loadPersons : Cmd Msg
loadPersons =
  Http.get
    { url = "data/person.json"
    , expect = Http.expectJson LoadPerson personDecoder
    }
