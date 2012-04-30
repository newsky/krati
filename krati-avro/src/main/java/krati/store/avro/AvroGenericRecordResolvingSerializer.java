/*
 * Copyright (c) 2010-2012 LinkedIn, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package krati.store.avro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import krati.io.SerializationException;
import krati.io.Serializer;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.ResolvingDecoder;

/**
 * AvroGenericRecordResolvingSerializer requires a writer schema and a reader schema.
 * The writer schema is used by {@link #serialize(GenericRecord)} and the reader
 * schema is used by {@link #deserialize(byte[])}.
 * 
 * @author jwu
 * @since 02/01, 2012
 */
public class AvroGenericRecordResolvingSerializer implements Serializer<GenericRecord> {
    private final Schema _writer;
    private final Schema _reader;
    private final Object _resolver;
    
    /**
     * Creates a new instance of AvroGenericRecordResolvingSerializer with the specified
     * writer and reader.
     * 
     * @param writer - the writer {@link Schema} used by {@link #serialize(GenericRecord)}.
     * @param reader - the reader {@link Schema} used by {@link #deserialize(byte[])}.
     * @throws IOException if the {@link ResolvingDecoder} failed to resolve the reader versus the writer.
     */
    public AvroGenericRecordResolvingSerializer(Schema writer, Schema reader) throws IOException {
        this._writer = writer;
        this._reader = reader;
        this._resolver = ResolvingDecoder.resolve(writer, reader);
    }
    
    @Override
    public GenericRecord deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null) {
            return null;
        }
        
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            Decoder decoder = DecoderFactory.defaultFactory().createBinaryDecoder(in, null);
            ResolvingDecoder resolvingDecoder = new ResolvingDecoder(_resolver, decoder); 
            GenericDatumReader<Record> datumReader = new GenericDatumReader<Record>(_reader);
            GenericData.Record record = new GenericData.Record(_reader);
            
            datumReader.read(record, resolvingDecoder);
            
            return record;
        } catch(Exception e) {
            throw new SerializationException("Failed to deserialize", e);
        }
    }
    
    @Override
    public byte[] serialize(GenericRecord record) throws SerializationException {
        if(record == null) {
            return null;
        }
        
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Encoder encoder = new BinaryEncoder(out); 
            GenericDatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<GenericRecord>(_writer);
            
            datumWriter.write(record, encoder);
            encoder.flush();
            
            return out.toByteArray();
        } catch(Exception e) {
            throw new SerializationException("Failed to serialize", e);
        }
    }
    
    /**
     * Gets the writer {@link Schema} used by {@link #serialize(GenericRecord)}.
     */
    public final Schema getWriter() {
        return _writer;
    }
    
    /**
     * Gets the reader {@link Schema} used by {@link #deserialize(byte[])}.
     */
    public final Schema getReader() {
        return _reader;
    }
    
    /**
     * Gets the opaque resolver generated by the {@link ResolvingDecoder}
     * based on the writer and the reader known to this serializer. 
     */
    public final Object getResolver() {
        return _resolver;
    }
}