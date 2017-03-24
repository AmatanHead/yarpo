#!/usr/bin/env escript
%% -*- erlang -*-
%%! -smp enable -sname imgcode -mnesia debug verbose

-record(image, {
  width = 0,
  height = 0,
  contents,
  headers
}).

-define(BPP, 3).

%% RLen + GLen + BLen must be <= 8
-define(RLen, 1).
-define(GLen, 1).
-define(BLen, 1).

dump(jpeg, _FileName, _Image) -> {error, not_implemented};
dump(png, _FileName, _Image) -> {error, not_implemented};
dump(bmp, FileName, Image) ->
  Contents = Image#image.contents,
  Headers = Image#image.headers,
  file:write_file(FileName, <<Headers/bitstring,Contents/bitstring>>).

load(jpeg, _FileName) -> {error, not_implemented};
load(png, _FileName) -> {error, not_implemented};
load(bmp, FileName) ->
  case file:read_file(FileName) of
    {ok, Contents} -> parse_bmp(Contents);
    Error  -> Error
  end.

parse_bmp(Contents) ->
  case Contents of
    <<"BM",_:64,Off:32/little,_:32,
      Width:32/signed-little,
      Height:32/signed-little,
      _Rest/binary>> ->
        Headers = binary_part(Contents,0,Off),
        PixelDataSize = size(Contents)-Off,
        io:fwrite("Head size: ~p, cont.size: ~p~n", [size(Headers), PixelDataSize]),
        PixelData = binary_part(Contents,Off,PixelDataSize),
        Image = #image{
          width = Width,
          height = Height,
          headers = Headers,
          contents = PixelData
        },
        {ok, Image}
    ;
    _ -> {error, wrong_format}
  end.

get_size(Image) ->
  W = abs(Image#image.width),
  H = abs(Image#image.height),
  {W, H}.

row_size(Image) ->
  {W, _} = get_size(Image),
  MeaningfullBytes = W * ?BPP,
  MeaningfullBytes + (MeaningfullBytes rem 4).

pixel_position(Image, {X,Y}) ->
  RowSize = row_size(Image),
  RowSize * Y + X * ?BPP.

get_pixel(Image, Point) ->
  Address = pixel_position(Image, Point),
  Contents = Image#image.contents,
  PixelData = binary_part(Contents, Address, ?BPP),
  <<B:8,G:8,R:8>> = PixelData,
  {R, G, B}.

set_pixel(Image, Point, {R, G, B}) ->
  Address = pixel_position(Image, Point) * 8,
  <<X:Address,_:8,_:8,_:8,Rest/bitstring>> = Image#image.contents,
  Image#image{contents = <<X:Address,B:8,G:8,R:8,Rest/bitstring>>}.

write_text(Image, Text) ->
  Bytes = unicode:characters_to_binary(Text),
  {W, H} = get_size(Image),
  TotalLen = (W * H - 1) * (?RLen + ?GLen + ?BLen),
  Len = min(TotalLen, bit_size(Bytes)),
  io:format("Message len is ~p bits, Max len is ~p bits~n", [bit_size(Bytes), TotalLen]),
  <<BytesTrimmed:Len/bitstring, _/bitstring>> = Bytes,
  io:format("Writing ~p bytes (~p bits)~n", [Len div 8, Len]),
  write_text_(Image, <<BytesTrimmed:Len/bitstring, 0:(TotalLen - Len)>>, 0).
write_text_(Image, Bytes, Offset) ->
  {W, H} = get_size(Image),
  X = Offset rem W,
  Y = Offset div W,
  if
    (Y >= H) ->
      io:format("Trim at (~p, ~p)~n", [X, Y]),
      Image;
    (bit_size(Bytes) >= ?RLen + ?GLen + ?BLen) ->
      {Ro, Go, Bo} = get_pixel(Image, {X, Y}),
      <<Rl:?RLen,Gl:?GLen,Bl:?BLen,Rest/bitstring>> = Bytes,
      <<R:8/integer>> = <<Ro:(8 - ?RLen), Rl:?RLen>>,
      <<G:8/integer>> = <<Go:(8 - ?GLen), Gl:?GLen>>,
      <<B:8/integer>> = <<Bo:(8 - ?BLen), Bl:?BLen>>,
      Image2 = set_pixel(Image, {X, Y}, {R, G, B}),
      write_text_(Image2, Rest, Offset + 1);
    true -> Image
  end.

read_text(Image) ->
  read_text_(Image, 0).
read_text_(Image, Offset) ->
  {W, H} = get_size(Image),
  X = Offset rem W,
  Y = Offset div W,
  if
    (X >= W) ->
      <<>>;
    (Y >= H) ->
      <<>>;
    true ->
      {R, G, B} = get_pixel(Image, {X, Y}),
      <<_:(8 - ?RLen), Rd:?RLen/integer>> = <<R>>,
      <<_:(8 - ?GLen), Gd:?GLen/integer>> = <<G>>,
      <<_:(8 - ?BLen), Bd:?BLen/integer>> = <<B>>,
      if
        true ->
          Rest = read_text_(Image, Offset + 1),
          <<Rd:?RLen, Gd:?GLen, Bd:?BLen, Rest/bitstring>>
      end
  end.

main([FileName, OutFileName, Text]) ->
  {ok, Image} = load(bmp, FileName),
  dump(bmp, OutFileName, write_text(Image, Text)),
  ok;
main([FileName]) ->
  {ok, Image} = load(bmp, FileName),
  io:format("'~s'~n", [read_text(Image)]),
  ok;
main(_) ->
  usage().

usage() ->
  io:format("usage: imgcode in out text -- write text to an image\nusage: imgcode in -- read from an image~n"),
  halt(1).
