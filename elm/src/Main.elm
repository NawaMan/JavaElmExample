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
  | Success (List Person)


init : () -> (Model, Cmd Msg)
init _ =
  (Loading, loadPersons)


type Msg
  = LoadPersons (Result Http.Error (List Person))


update : Msg -> Model -> (Model, Cmd Msg)
update msg model =
  case msg of
    LoadPersons result ->
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
    , viewPersons model
    ]


viewPersons : Model -> Html Msg
viewPersons model =
  case model of
    Failure ->
      div []
        [ text "I could not load `person.json`. " ]

    Loading ->
      text "Loading..."

    Success persons ->
      div [ class "persons" ]
          [ ul []
            (persons
              |> List.map (\person -> viewPerson person)
            )
          ]


viewPerson : Person -> Html Msg
viewPerson person = 
  div [ class "person" ]
  [ text person.firstName
  , text " "
  , text person.lastName
  ]



-- HTTP


loadPersons : Cmd Msg
loadPersons =
  Http.get
    { url = "data/persons.json"
    , expect = Http.expectJson LoadPersons personListDecoder
    }
