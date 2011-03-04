package com.c9a.catalog.utils;

import java.util.List;

import com.c9a.catalog.entities.CatalogCollection;
import com.c9a.catalog.entities.Document;
import com.c9a.catalog.entities.ExtendedAttribute;
import com.c9a.catalog.entities.Permission;
import com.c9a.catalog.entities.Reference;
import com.c9a.catalog.web.CatalogController;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public class CatalogGPBUtils {
	
	/**
	 * This method will generate a CatalogCollectionResponse google protocol buffer for a given set of collections, response, and message
	 * @param responseEnum						The enum of the status to sue in the response
	 * @param responseMessage					The message to include in the response
	 * @param catalogCollections				The one to many collections to add to the response
	 * @return									The generated protocol buffer
	 * @throws InvalidProtocolBufferException	Thrown when the buffer can not be built with the provided data
	 */
	public static com.c9a.buffers.CatalogServiceResponseProtocalBuffer.CatalogCollectionResponse.Builder generateCatalogCollectionResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode responseEnum, String responseMessage, CatalogCollection catalogCollection) throws InvalidProtocolBufferException {
		com.c9a.buffers.CatalogServiceResponseProtocalBuffer.CatalogCollectionResponse.Builder catalogCollectionResponse = com.c9a.buffers.CatalogServiceResponseProtocalBuffer.CatalogCollectionResponse.newBuilder();
		catalogCollectionResponse.setVersion(CatalogController.VERSION_FIELD);
		
		catalogCollectionResponse.addCollections(generateCatalogCollectionProtoBuf(catalogCollection));
		
		catalogCollectionResponse.setResponse(generateResponseProtoBuf(responseEnum, responseMessage));
		return catalogCollectionResponse;
	}
	
	public static com.c9a.buffers.CatalogServiceResponseProtocalBuffer.CatalogCollectionResponse.Builder generateCatalogCollectionResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode responseEnum, String responseMessage, List<CatalogCollection> catalogCollections) throws InvalidProtocolBufferException {
		com.c9a.buffers.CatalogServiceResponseProtocalBuffer.CatalogCollectionResponse.Builder catalogCollectionResponse = com.c9a.buffers.CatalogServiceResponseProtocalBuffer.CatalogCollectionResponse.newBuilder();
		catalogCollectionResponse.setVersion(CatalogController.VERSION_FIELD);
		
		for(CatalogCollection cc : catalogCollections){
			catalogCollectionResponse.addCollections(generateCatalogCollectionProtoBuf(cc));
		}
		
		catalogCollectionResponse.setResponse(generateResponseProtoBuf(responseEnum, responseMessage));
		return catalogCollectionResponse;
	}
	
	/**
	 * This method will build a CatalogCollectionShallow Google Protocol buffer from a given CatalogCollection object
	 * @param catalogCollection		The CatalogCollection to use in the translation
	 * @return						The Google Protocol buffer of the CatalogCollection
	 */
	public static com.c9a.buffers.CatalogServiceResponseProtocalBuffer.CatalogCollectionShallow.Builder generateCatalogCollectionProtoBuf(CatalogCollection catalogCollection) {
		com.c9a.buffers.CatalogServiceResponseProtocalBuffer.CatalogCollectionShallow.Builder shallow = com.c9a.buffers.CatalogServiceResponseProtocalBuffer.CatalogCollectionShallow.newBuilder();
		shallow.setName(catalogCollection.getName());
		shallow.setUserId(catalogCollection.getOwner());
		shallow.setPartitionId(catalogCollection.getPartitionId());
		shallow.setUniqueId(catalogCollection.getUniqueId());
		shallow.setCreateDateInMill(catalogCollection.getCreateDate().getTime());
		shallow.setLastUpdateInMill(catalogCollection.getLastModifiedDate().getTime());
		shallow.setArchived(catalogCollection.isArchived() ? 1L : 0L);
		if(catalogCollection.getParentCollection() != null){
			shallow.setParentCollection(generateKeyValuePair(catalogCollection.getParentCollection().getUniqueId(), catalogCollection.getParentCollection().getName()));
		}
		for(CatalogCollection nestedCollection : catalogCollection.getNestedCollections()){
			shallow.addNestedColelctions(generateKeyValuePair(nestedCollection.getUniqueId(), nestedCollection.getName()));
		}
		for(CatalogCollection sharedTo : catalogCollection.getSharedTo()){
			shallow.addSharedToCollections(generateKeyValuePair(sharedTo.getUniqueId(), sharedTo.getName()));
		}
		if(catalogCollection.getSharedFrom() != null){
			shallow.setSharedFromCollection(generateKeyValuePair(catalogCollection.getSharedFrom().getUniqueId(), catalogCollection.getSharedFrom().getName()));
		}
		for(ExtendedAttribute attribute : catalogCollection.getAttributes()){
			shallow.addAttributes(generateKeyValuePair(attribute.getKey(), attribute.getValue()));
		}
		for(Permission permission : catalogCollection.getPermissions()){
			shallow.addPermissions(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.Permission.valueOf(permission.getId().intValue()));
		}
		for(Reference ref : catalogCollection.getReferences()){
			shallow.addReferences(generateReferenceProtoBuf(ref));
		}
		return shallow;
	}

	private static com.c9a.buffers.CatalogServiceResponseProtocalBuffer.Reference.Builder generateReferenceProtoBuf(Reference ref) {
		com.c9a.buffers.CatalogServiceResponseProtocalBuffer.Reference.Builder protoBufRef = com.c9a.buffers.CatalogServiceResponseProtocalBuffer.Reference.newBuilder();
		protoBufRef.setUniqueId(ref.getUniqueId());
		protoBufRef.setName(ref.getName());
		protoBufRef.setUserId(ref.getOwner());
		protoBufRef.setPartitionId(ref.getPartitionId());
		protoBufRef.setCreateDateInMill(ref.getCreateDate().getTime());
		protoBufRef.setLastUpdateInMill(ref.getLastModifiedDate().getTime());
		protoBufRef.setParentCollection(generateKeyValuePair(ref.getParentCollection().getUniqueId(), ref.getParentCollection().getName()));
		protoBufRef.setDocument(generateDocumentProtoBuf(ref.getDocument()));
		protoBufRef.setArchived(ref.isArchived() ? 1L : 0L);
		for(ExtendedAttribute ea : ref.getAttributes()){
			protoBufRef.addAttributes(generateKeyValuePair(ea.getKey(), ea.getValue()));
		}
		for(Permission permission : ref.getPermissions()){
			protoBufRef.addPermissions(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.Permission.valueOf(permission.getId().intValue()));
		}
		return protoBufRef;
	}

	private static com.c9a.buffers.CatalogServiceResponseProtocalBuffer.Document.Builder generateDocumentProtoBuf(Document document) {
		com.c9a.buffers.CatalogServiceResponseProtocalBuffer.Document.Builder protoBufDoc = com.c9a.buffers.CatalogServiceResponseProtocalBuffer.Document.newBuilder();
		protoBufDoc.setUniqueId(document.getUniqueId());
		protoBufDoc.setUserId(document.getOwner());
		protoBufDoc.setPartitionId(document.getPartitionId());
		protoBufDoc.setCreateDateInMill(document.getCreateDate().getTime());
		protoBufDoc.setLastUpdateInMill(document.getLastModifiedDate().getTime());
		protoBufDoc.setName(document.getName());
		if(document.getDescription() != null){
			protoBufDoc.setDescription(document.getDescription());
		}
		protoBufDoc.setContent(ByteString.copyFrom(document.getPayload()));
		for(ExtendedAttribute ea : document.getAttributes()){
			protoBufDoc.addAttributes(generateKeyValuePair(ea.getKey(), ea.getValue()));
		}
		return protoBufDoc;
	}

	private static com.c9a.buffers.CatalogServiceResponseProtocalBuffer.KeyValuePair.Builder generateKeyValuePair(String key, String value) {
		com.c9a.buffers.CatalogServiceResponseProtocalBuffer.KeyValuePair.Builder keyValuePair = com.c9a.buffers.CatalogServiceResponseProtocalBuffer.KeyValuePair.newBuilder();
		keyValuePair.setKey(key);
		keyValuePair.setValue(value);
		return keyValuePair;
	}

	public static com.c9a.buffers.CatalogServiceResponseProtocalBuffer.CatalogCollectionResponse.Builder generateCatalogCollectionResponseProtoBufError(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode responseEnum, String message) {
		com.c9a.buffers.CatalogServiceResponseProtocalBuffer.CatalogCollectionResponse.Builder catalogCollectionResponse = com.c9a.buffers.CatalogServiceResponseProtocalBuffer.CatalogCollectionResponse.newBuilder();
		catalogCollectionResponse.setVersion(CatalogController.VERSION_FIELD);
		catalogCollectionResponse.setResponse(generateResponseProtoBuf(responseEnum, message));
		return catalogCollectionResponse;
	}

	public static com.c9a.buffers.CatalogServiceResponseProtocalBuffer.Response.Builder generateResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode responseCode, String message) {
		com.c9a.buffers.CatalogServiceResponseProtocalBuffer.Response.Builder response = com.c9a.buffers.CatalogServiceResponseProtocalBuffer.Response.newBuilder();
		response.setVersion(CatalogController.VERSION_FIELD);
		response.setResponseCode(responseCode);
		response.setResponseMessage(message);
		return response;
	}

	public static com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ReferenceResponse.Builder generateReferenceResponseProtoBuf(Reference ref, com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode responseCode, String responseMessage) {
		com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ReferenceResponse.Builder refProtoBuf = com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ReferenceResponse.newBuilder();
		refProtoBuf.setVersion(CatalogController.VERSION_FIELD);
		refProtoBuf.setReference(generateReferenceProtoBuf(ref));
		refProtoBuf.setResponse(generateResponseProtoBuf(responseCode, responseMessage));
		return refProtoBuf;
	}

}
