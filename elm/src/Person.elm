-- Generated by FunctionJ.io ( https://functionalj.io ) on 2021-07-22T15:14:39.812915 
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

type alias Person = 
    { id : Maybe String
    , firstName : String
    , lastName : String
    , nickName : Maybe String
    }


personEncoder : Person -> Json.Encode.Value
personEncoder person = 
    Json.Encode.object
        [ ( "id", Maybe.withDefault Json.Encode.null (Maybe.map Json.Encode.string person.id) )
        , ( "firstName", Json.Encode.string person.firstName )
        , ( "lastName", Json.Encode.string person.lastName )
        , ( "nickName", Maybe.withDefault Json.Encode.null (Maybe.map Json.Encode.string person.nickName) )
        ]


personDecoder : Json.Decode.Decoder Person
personDecoder = 
    Json.Decode.succeed Person
        |> Json.Decode.Pipeline.optional "id" (Json.Decode.maybe Json.Decode.string) Nothing
        |> Json.Decode.Pipeline.required "firstName" Json.Decode.string
        |> Json.Decode.Pipeline.required "lastName" Json.Decode.string
        |> Json.Decode.Pipeline.optional "nickName" (Json.Decode.maybe Json.Decode.string) Nothing


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
