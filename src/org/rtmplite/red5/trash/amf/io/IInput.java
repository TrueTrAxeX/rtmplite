package org.rtmplite.red5.trash.amf.io;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;
import java.util.Vector;

import org.apache.mina.util.byteaccess.ByteArray;
import org.w3c.dom.Document;

/**
 * Interface for Input which defines the contract methods which are
 * to be implemented. Input object provides
 * ways to read primitives, complex object and object references from byte buffer.
 * 
 * @author The Red5 Project
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public interface IInput {
	/**
	 * Read type of data
	 * @return         Type of data as byte
	 */
	byte readDataType();

	/**
	 * Read a string without the string type header.
	 * 
	 * @return         String
	 */
	String getString();

	/**
	 * Read Null data type
	 * @param target target type
	 * @return         Null datatype (AS)
	 */
	Object readNull(Type target);

	/**
	 * Read Boolean value
	 * @param target target type
	 * @return         Boolean
	 */
	Boolean readBoolean(Type target);

	/**
	 * Read Number object
	 * @param target target type
	 * @return         Number
	 */
	Number readNumber(Type target);

	/**
	 * Read String object
	 * @param target target type 
	 * @return         String
	 */
	String readString(Type target);

	/**
	 * Read date object
	 * @param target target type 
	 * @return         Date
	 */
	Date readDate(Type target);

	/**
	 * Read an array. This can result in a List or Map being
	 * deserialized depending on the array type found.
	 * 
	 * @param target target type
	 * @return		   array
	 */
	Object readArray(Type target);

	/**
	 * Read a map containing key - value pairs. This can result
	 * in a List or Map being deserialized depending on the
	 * map type found.
	 * 
	 * @param target target type
	 * @return		   Map
	 */
	Object readMap(Type target);

	/**
	 * Read an object.
	 * 
	 * @param target target type
	 * @return		   object
	 */
	Object readObject(Type target);

	/**
	 * Read XML document
	 * 
	 * @param target target type
	 * @return       XML DOM document
	 */
	Document readXML(Type target);

	/**
	 * Read custom object
	 * 
	 * @param target target type
	 * @return          Custom object
	 */
	Object readCustom(Type target);

	/**
	 * Read ByteArray object.
	 * 
	 * @param target target type
	 * @return		ByteArray object
	 */
	ByteArray readByteArray(Type target);

	/**
	 * Read reference to Complex Data Type. Objects that are collaborators (properties) of other
	 * objects must be stored as references in map of id-reference pairs.
	 * 
	 * @param target target type
	 * @return object
	 */
	Object readReference(Type target);

	/**
	 * Clears all references
	 */
	void clearReferences();

	/**
	 * Read key - value pairs. This is required for the RecordSet deserializer.
	 * 
	 * @param deserializer deserializer
	 * @return key-value pairs
	 */
	Map<String, Object> readKeyValues();

	/**
	 * Read Vector<int> object.
	 * 
	 * @return Vector<Integer>
	 */
	Vector<Integer> readVectorInt();

	/**
	 * Read Vector<uint> object.
	 * 
	 * @return Vector<Long>
	 */
	Vector<Long> readVectorUInt();

	/**
	 * Read Vector<Number> object.
	 * 
	 * @return Vector<Double>
	 */
	Vector<Double> readVectorNumber();

	/**
	 * Read Vector<Object> object.
	 * 
	 * @return Vector<Object>
	 */
	Vector<Object> readVectorObject();

}