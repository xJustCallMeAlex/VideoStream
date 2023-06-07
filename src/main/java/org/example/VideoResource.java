package org.example;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Date;

@Path("/video")
public class VideoResource {
    private final int chunk_size = 1024 * 1024 * 2;
    @GET
    @Produces("video/mp4")
    public Response hello(@HeaderParam("Range") String range, @QueryParam("path") String path) throws IOException {

        File file = new File("videos/" + path + ".mp4");
        if (!file.exists()) {
            return Response.serverError().build();
        }
        if (range == null) {
            StreamingOutput streamer = output -> {
                try (FileChannel inputChannel = new FileInputStream( file ).getChannel();
                     WritableByteChannel outputChannel = Channels.newChannel( output ) ) {

                    inputChannel.transferTo( 0, inputChannel.size(), outputChannel );
                }
                catch( IOException io ) {
                    System.out.println("Error");
                }
            };

            return Response.ok( streamer )
                    .status( Response.Status.OK )
                    .header( HttpHeaders.CONTENT_LENGTH, file.length() )
                    .build();
        }
        String[] ranges = range.split( "=" )[1].split( "-" );

        int from = Integer.parseInt( ranges[0] );

        // Chunk media if the range upper bound is unspecified
        int to = chunk_size + from;

        if ( to >= file.length() ) {
            to = (int) ( file.length() - 1 );
        }

        // uncomment to let the client decide the upper bound
        // we want to send 2 MB chunks all the time
        //if ( ranges.length == 2 ) {
        //    to = Integer.parseInt( ranges[1] );
        //}

        final String responseRange = String.format( "bytes %d-%d/%d", from, to, file.length() );



        final RandomAccessFile raf = new RandomAccessFile( file, "r" );
        raf.seek( from );
        final int len = to - from + 1;
        final MediaStreamer mediaStreamer = new MediaStreamer( len, raf );
        return Response.ok( mediaStreamer )
                .status( Response.Status.PARTIAL_CONTENT )
                .header( "Accept-Ranges", "bytes" )
                .header( "Content-Range", responseRange )
                .header( HttpHeaders.CONTENT_LENGTH, mediaStreamer. getLength() )
                .header( HttpHeaders.LAST_MODIFIED, new Date( file.lastModified() ) )
                .build();


    }
}
