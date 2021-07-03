module Person exposing
    ( Person
    , personEncoder
    , personDecoder
    , encodePerson
    , decodePerson
    , personListEncoder
    , personListDecoder
    , encodePersonList
    , decodePersonList
    )

import Json.Decode
import Json.Decode.Pipeline
import Json.Encode


-- elm install elm/json
-- elm install NoRedInk/elm-json-decode-pipeline

import Html

main =
  Html.text "Hello!"


type alias Person = 
    { firstName : String
    , lastName : String
    }


personEncoder : Person -> Json.Encode.Value
personEncoder person = 
    Json.Encode.object
        [ ( "firstName", Json.Encode.string person.firstName )
        , ( "lastName", Json.Encode.string person.lastName )
        ]


personDecoder : Json.Decode.Decoder Person
personDecoder = 
    Json.Decode.succeed Person
        |> Json.Decode.Pipeline.required "firstName" Json.Decode.string
        |> Json.Decode.Pipeline.required "lastName" Json.Decode.string


encodePerson : Person -> Int -> String
encodePerson person indent = 
    personEncoder person |> Json.Encode.encode indent


decodePerson : String -> Result Json.Decode.Error Person
decodePerson = 
    Json.Decode.decodeString personDecoder


personListEncoder : List Person -> Json.Encode.Value
personListEncoder personList = 
    Json.Encode.list personEncoder personList


personListDecoder : Json.Decode.Decoder (List Person)
personListDecoder = 
    Json.Decode.list personDecoder


encodePersonList : List Person -> Int -> String
encodePersonList personList indent = 
    personListEncoder personList |> Json.Encode.encode indent


decodePersonList : String -> Result Json.Decode.Error (List Person)
decodePersonList = 
    Json.Decode.decodeString personListDecoder
