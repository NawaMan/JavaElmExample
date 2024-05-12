-- Generated by FunctionJ.io ( https://functionalj.io ) on 2024-05-12T11:37:29.523967405 
module Cape exposing
    ( Cape(..)
    , capeEncoder
    , capeDecoder
    , encodeCape
    , decodeCape
    , capeListEncoder
    , capeListDecoder
    , encodeCapeList
    , decodeCapeList
    )

import Json.Decode
import Json.Decode.Pipeline
import Json.Encode




-- elm install elm/json
-- elm install NoRedInk/elm-json-decode-pipeline

type Cape
    = Color String
    | None


capeEncoder : Cape -> Json.Encode.Value
capeEncoder cape = 
    case cape of
        Color color ->
            Json.Encode.object
                [ ( "__tagged", Json.Encode.string "Color" )
                , ( "color", Json.Encode.string color )
                ]
        None  ->
            Json.Encode.object
                [ ( "__tagged", Json.Encode.string "None" )
                ]


capeDecoder : Json.Decode.Decoder Cape
capeDecoder = 
    Json.Decode.field "__tagged" Json.Decode.string
        |> Json.Decode.andThen
            (\str ->
                case str of
                    "Color" ->
                        Json.Decode.succeed Color
                            |> Json.Decode.Pipeline.required "color" Json.Decode.string
                    
                    "None" ->
                        Json.Decode.succeed None
                    
                    somethingElse ->
                        Json.Decode.fail <| "Unknown tagged: " ++ somethingElse
    )


encodeCape : Cape -> Int -> String
encodeCape cape indent = 
    capeEncoder cape |> Json.Encode.encode indent


decodeCape : String -> Result Json.Decode.Error Cape
decodeCape = 
    Json.Decode.decodeString capeDecoder


capeListEncoder : List Cape -> Json.Encode.Value
capeListEncoder capeList = 
    Json.Encode.list capeEncoder capeList


capeListDecoder : Json.Decode.Decoder (List Cape)
capeListDecoder = 
    Json.Decode.list capeDecoder


encodeCapeList : List Cape -> Int -> String
encodeCapeList capeList indent = 
    capeListEncoder capeList |> Json.Encode.encode indent


decodeCapeList : String -> Result Json.Decode.Error (List Cape)
decodeCapeList = 
    Json.Decode.decodeString capeListDecoder
