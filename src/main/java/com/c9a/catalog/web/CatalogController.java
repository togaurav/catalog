package com.c9a.catalog.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.c9a.catalog.entities.CatalogCollection;
import com.c9a.catalog.entities.Reference;
import com.c9a.catalog.exception.CollectionModificationException;
import com.c9a.catalog.exception.CollectionNotFoundException;
import com.c9a.catalog.exception.CollectionSharingException;
import com.c9a.catalog.exception.InvalidCatalogNameException;
import com.c9a.catalog.exception.ReferenceModificationException;
import com.c9a.catalog.exception.ReferenceNotFoundException;
import com.c9a.catalog.exception.ReferenceSharingException;
import com.c9a.catalog.service.ICatalogService;
import com.c9a.catalog.utils.CatalogGPBUtils;
import com.c9a.service.spring.GoogleProtoclBufferRequestContentUtils;

@Controller("catalogController")
public class CatalogController {

	@Autowired
	@Qualifier("catalogService")
	private ICatalogService catalogService;
	
	public static final int VERSION_FIELD = 1;
	
	// Collections
	public static final String ROOT_APPLICATION_COLLECTION_FOR_USER_REQUEST = "/getRootApplicationCollectionForUser";
	public static final String ROOT_PARTITION_COLLECTION_REQUEST = "/getRootPartitionCollection";
	public static final String ROOT_USER_COLLECTION_REQUEST = "/getUserRootCatalog";
	
	public static final String GET_COLLECTION_REQUEST = "/getCollection";
	public static final String ADD_COLLECTION_REQUEST = "/addCollection";
	public static final String SHARE_COLLECTION_REQUEST = "/shareCollection";
	public static final String DELETE_COLLECTION_REQUEST = "/deleteCollection";
	public static final String MODIFY_COLLECTION_REQUEST = "/modifyCollection";

	// References
	public static final String GET_REFERENCE_REQUEST = "/getReference";
	public static final String SHARE_REFERENCE_REQUEST = "/shareReference";
	public static final String DELETE_REFERENCE_REQUEST = "/deleteReference";
	public static final String MODIFY_REFERENCE_REQUEST = "/modifyReference";

	// Documents
	public static final String ADD_DOCUMENT_REQUEST = "/addDocument";

	private static final String VIEW_API_REQUEST = "/api";

	public static final Map<String, RequestResponse> SERVICE_SUPPORT_MAP = new HashMap<String, RequestResponse>() {
		private static final long serialVersionUID = 7407042581509693821L;
		{
			put(ROOT_APPLICATION_COLLECTION_FOR_USER_REQUEST, new RequestResponse(
					com.c9a.buffers.CatalogServiceRequestProtocalBuffer.GetRootApplicationCollectionForUserRequest.newBuilder(), 
					com.c9a.buffers.CatalogServiceResponseProtocalBuffer.CatalogCollectionResponse.newBuilder())
			);
			
			put(ROOT_PARTITION_COLLECTION_REQUEST, new RequestResponse(
					com.c9a.buffers.CatalogServiceRequestProtocalBuffer.GetRootPartitionCollectionRequest.newBuilder(), 
					com.c9a.buffers.CatalogServiceResponseProtocalBuffer.CatalogCollectionResponse.newBuilder())
			);
			
			put(ROOT_USER_COLLECTION_REQUEST, new RequestResponse(
					com.c9a.buffers.CatalogServiceRequestProtocalBuffer.GetUserRootCollectionRequest.newBuilder(), 
					com.c9a.buffers.CatalogServiceResponseProtocalBuffer.CatalogCollectionResponse.newBuilder())
			);
			
			put(GET_COLLECTION_REQUEST, new RequestResponse(
					com.c9a.buffers.CatalogServiceRequestProtocalBuffer.GetCollectionRequest.newBuilder(),
					com.c9a.buffers.CatalogServiceResponseProtocalBuffer.CatalogCollectionResponse.newBuilder()
			));
			
			put(ADD_COLLECTION_REQUEST, new RequestResponse(
					com.c9a.buffers.CatalogServiceRequestProtocalBuffer.AddCollectionRequest.newBuilder(),
					com.c9a.buffers.CatalogServiceResponseProtocalBuffer.CatalogCollectionResponse.newBuilder()
			));
			
			put(SHARE_COLLECTION_REQUEST, new RequestResponse(
					com.c9a.buffers.CatalogServiceRequestProtocalBuffer.ShareCollectionRequest.newBuilder(),
					com.c9a.buffers.CatalogServiceResponseProtocalBuffer.Response.newBuilder()
			));
			
			put(SHARE_REFERENCE_REQUEST, new RequestResponse(
					com.c9a.buffers.CatalogServiceRequestProtocalBuffer.ShareReferenceRequest.newBuilder(),
					com.c9a.buffers.CatalogServiceResponseProtocalBuffer.Response.newBuilder()
			));
			
			put(DELETE_COLLECTION_REQUEST, new RequestResponse(
					com.c9a.buffers.CatalogServiceRequestProtocalBuffer.DeleteCatalogCollectionRequest.newBuilder(),
					com.c9a.buffers.CatalogServiceResponseProtocalBuffer.Response.newBuilder()
			));
			
			put(GET_REFERENCE_REQUEST, new RequestResponse(
					com.c9a.buffers.CatalogServiceRequestProtocalBuffer.GetReferenceRequest.newBuilder(),
					com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ReferenceResponse.newBuilder()
			));
			
			put(ADD_DOCUMENT_REQUEST, new RequestResponse(
					com.c9a.buffers.CatalogServiceRequestProtocalBuffer.AddDocumentRequest.newBuilder(),
					com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ReferenceResponse.newBuilder()
			));
			
			put(DELETE_REFERENCE_REQUEST, new RequestResponse(
					com.c9a.buffers.CatalogServiceRequestProtocalBuffer.DeleteReferenceRequest.newBuilder(),
					com.c9a.buffers.CatalogServiceResponseProtocalBuffer.Response.newBuilder()
			));
			
			put(MODIFY_COLLECTION_REQUEST, new RequestResponse(
					com.c9a.buffers.CatalogServiceRequestProtocalBuffer.ModifyCatalogCollectionRequest.newBuilder(),
					com.c9a.buffers.CatalogServiceResponseProtocalBuffer.CatalogCollectionResponse.newBuilder()
			));
			
			put(MODIFY_REFERENCE_REQUEST, new RequestResponse(
					com.c9a.buffers.CatalogServiceRequestProtocalBuffer.ModifyReferenceRequest.newBuilder(),
					com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ReferenceResponse.newBuilder()
			));
		}
	};
	
	@RequestMapping(value = VIEW_API_REQUEST, method = { RequestMethod.POST, RequestMethod.GET })
	public void listAllSupportedActions(
			HttpServletRequest request, 
			HttpServletResponse response, 	
			@RequestParam(value = "url", required = false) String url) throws IOException {
		
		if(url == null){
			StringBuilder sb = new StringBuilder();
			sb.append("<html><body>");
			for(String key : SERVICE_SUPPORT_MAP.keySet()){
				RequestResponse rr = SERVICE_SUPPORT_MAP.get(key);
				sb.append("<div>url : ").append(key).append("</div>");
				sb.append("<div style=\"padding-left:15px>\">").append(rr.getRequest().getDescriptorForType().getFullName()).append(" : ").append(rr.getResponse().getDescriptorForType().getFullName()).append("</div>");
			}
			sb.append("</body></hhtml>");
			response.getOutputStream().write(sb.toString().getBytes());
			return;
		}
		
		apiRequested(response, true, url);
		return;
	}
	
	@RequestMapping(value=ROOT_USER_COLLECTION_REQUEST, method = { RequestMethod.POST })
	public void rootUserCollectionRequest(
			HttpServletRequest request, 
			HttpServletResponse response, 	
			@RequestParam(value = "api", required = false) boolean api) throws IOException {
		
		if (apiRequested(response, api, ROOT_USER_COLLECTION_REQUEST))
			return;

		com.c9a.buffers.CatalogServiceRequestProtocalBuffer.GetUserRootCollectionRequest rootPartitionCollectionRequest = com.c9a.buffers.CatalogServiceRequestProtocalBuffer.GetUserRootCollectionRequest.parseFrom(GoogleProtoclBufferRequestContentUtils.getContentForRequest(request));
		
		CatalogCollection rootPartitionCollection = catalogService
															.getRootCollectionForUser(
																	rootPartitionCollectionRequest.getUserId(), 
																	rootPartitionCollectionRequest.getPartitionId());
		
		CatalogGPBUtils
					.generateCatalogCollectionResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.SUCCESS, "", rootPartitionCollection)
					.build()
					.writeTo(response.getOutputStream());
		return;
	}
	
	@RequestMapping(value=ROOT_PARTITION_COLLECTION_REQUEST, method = { RequestMethod.POST })
	public void rootPartitionCollectionRequest(
			HttpServletRequest request, 
			HttpServletResponse response, 	
			@RequestParam(value = "api", required = false) boolean api) throws IOException {
		
		if (apiRequested(response, api, ROOT_APPLICATION_COLLECTION_FOR_USER_REQUEST))
			return;

		com.c9a.buffers.CatalogServiceRequestProtocalBuffer.GetRootPartitionCollectionRequest rootPartitionCollectionRequest = com.c9a.buffers.CatalogServiceRequestProtocalBuffer.GetRootPartitionCollectionRequest.parseFrom(GoogleProtoclBufferRequestContentUtils.getContentForRequest(request));
		
		List<CatalogCollection> rootPartitionCollection = catalogService
														.getRootCollectionForPartition(rootPartitionCollectionRequest.getPartitionId());
		
		CatalogGPBUtils
					.generateCatalogCollectionResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.SUCCESS, "", rootPartitionCollection)
					.build()
					.writeTo(response.getOutputStream());
		return;
	}

	@RequestMapping(value=ROOT_APPLICATION_COLLECTION_FOR_USER_REQUEST, method = { RequestMethod.POST })
	public void rootApplicationCollectionRequest(
			HttpServletRequest request, 
			HttpServletResponse response, 	
			@RequestParam(value = "api", required = false) boolean api) throws IOException {
		
		if (apiRequested(response, api, ROOT_APPLICATION_COLLECTION_FOR_USER_REQUEST))
			return;

		com.c9a.buffers.CatalogServiceRequestProtocalBuffer.GetRootApplicationCollectionForUserRequest rootApplicationCollectionForUserRequest = com.c9a.buffers.CatalogServiceRequestProtocalBuffer.GetRootApplicationCollectionForUserRequest.parseFrom(GoogleProtoclBufferRequestContentUtils.getContentForRequest(request));
		
		CatalogCollection rootAplicationCollection = catalogService
															.getRootApplicationCollectionForUser(
																	rootApplicationCollectionForUserRequest.getUserId(), 
																	rootApplicationCollectionForUserRequest.getPartitionId(), 
																	rootApplicationCollectionForUserRequest.getApplicationName());
		
		CatalogGPBUtils
					.generateCatalogCollectionResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.SUCCESS, "", rootAplicationCollection)
					.build()
					.writeTo(response.getOutputStream());
		return;
	}
	
	@RequestMapping(value = GET_COLLECTION_REQUEST, method = { RequestMethod.POST })
	public void getCollection(
			HttpServletRequest request, 
			HttpServletResponse response, 	
			@RequestParam(value = "api", required = false) boolean api) throws IOException {

		if (apiRequested(response, api, GET_COLLECTION_REQUEST))
			return;
		
		com.c9a.buffers.CatalogServiceRequestProtocalBuffer.GetCollectionRequest collectionRequest = com.c9a.buffers.CatalogServiceRequestProtocalBuffer.GetCollectionRequest.parseFrom(GoogleProtoclBufferRequestContentUtils.getContentForRequest(request));
		
		try {
			CatalogCollection catalogCollection = catalogService.getCollection(
																		collectionRequest.getUserId(), 
																		collectionRequest.getPartitionId(), 
																		collectionRequest.getUniqueId());
			CatalogGPBUtils
				.generateCatalogCollectionResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.SUCCESS, "", catalogCollection)
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (CollectionNotFoundException e) {
			CatalogGPBUtils
				.generateCatalogCollectionResponseProtoBufError(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.COLLECTION_NOT_FOUND, "The collection was not found with the give unique id : " + collectionRequest.getUniqueId())
				.build()
				.writeTo(response.getOutputStream());
			return;
		}
	}

	@RequestMapping(value = ADD_COLLECTION_REQUEST, method = { RequestMethod.POST })
	public void addCollection(
			HttpServletRequest request, 
			HttpServletResponse response, 	
			@RequestParam(value = "api", required = false) boolean api) throws IOException {

		if (apiRequested(response, api, ADD_COLLECTION_REQUEST))
			return;
		
		com.c9a.buffers.CatalogServiceRequestProtocalBuffer.AddCollectionRequest addNewCollectionRequest = com.c9a.buffers.CatalogServiceRequestProtocalBuffer.AddCollectionRequest.parseFrom(GoogleProtoclBufferRequestContentUtils.getContentForRequest(request));
		
		try {
			Set<Long> permissions = createPermissionListFromProtoBufEnumList(addNewCollectionRequest.getPermissionsList());
			Map<String, String> attributes = createNewMap(addNewCollectionRequest.getAttributesList());
			CatalogCollection catalogCollection = catalogService.addCollection(
																	addNewCollectionRequest.getUserId(), 
																	addNewCollectionRequest.getPartitionId(), 
																	addNewCollectionRequest.getCollectionName(), 
																	addNewCollectionRequest.getParentCatalogCollectionId(), 
																	permissions, 
																	attributes);
			CatalogGPBUtils
				.generateCatalogCollectionResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.SUCCESS, "", catalogCollection)
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (CollectionNotFoundException e) {
			CatalogGPBUtils
				.generateCatalogCollectionResponseProtoBufError(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.COLLECTION_NOT_FOUND, "The parent collection was not found with the give unique id : " + addNewCollectionRequest.getParentCatalogCollectionId())
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (CollectionModificationException e) {
			CatalogGPBUtils
				.generateCatalogCollectionResponseProtoBufError(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.COLLECTION_CAN_NOT_BE_MODIFIED, "The parent collection with unique id : " + addNewCollectionRequest.getParentCatalogCollectionId() + " can not be modified : ")
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (InvalidCatalogNameException e) {
			CatalogGPBUtils
				.generateCatalogCollectionResponseProtoBufError(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.CATALOG_NAME_INVALID, "The supplied name is not valid for the catalog")
				.build()
				.writeTo(response.getOutputStream());
			return;
		}
	}

	@RequestMapping(value = SHARE_COLLECTION_REQUEST, method = { RequestMethod.POST })
	public void shareCollection(
			HttpServletRequest request, 
			HttpServletResponse response, 	
			@RequestParam(value = "api", required = false) boolean api) throws IOException {

		if (apiRequested(response, api, SHARE_COLLECTION_REQUEST))
			return;
		
		com.c9a.buffers.CatalogServiceRequestProtocalBuffer.ShareCollectionRequest shareCollectionForUserRequest = com.c9a.buffers.CatalogServiceRequestProtocalBuffer.ShareCollectionRequest.parseFrom(GoogleProtoclBufferRequestContentUtils.getContentForRequest(request));
		
		try {
			Map<String, String> userAndPartitionsToShareWith = createNewMap(shareCollectionForUserRequest.getUsersAndPartitionsToShareWithList());
			Set<Long> sharePermissionIds = createPermissionListFromProtoBufEnumList(shareCollectionForUserRequest.getPermissionsList());
			catalogService.shareCollection(
								shareCollectionForUserRequest.getUserId(), 
								shareCollectionForUserRequest.getPartitionId(), 
								shareCollectionForUserRequest.getApplicationName(), 
								shareCollectionForUserRequest.getCollectionUniqueId(), 
								userAndPartitionsToShareWith, 
								sharePermissionIds);
			CatalogGPBUtils
				.generateResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.SUCCESS, "")
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (CollectionNotFoundException e) {
			CatalogGPBUtils
				.generateResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.COLLECTION_NOT_FOUND, "The collection was not found with the give unique id : " + shareCollectionForUserRequest.getCollectionUniqueId())
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (CollectionSharingException e) {
			CatalogGPBUtils
				.generateResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.COLLECTION_CAN_NOT_BE_SHARED, "The collection does not have the needed permissions to be shared.")
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (InvalidCatalogNameException e) {
			CatalogGPBUtils
				.generateResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.CATALOG_NAME_INVALID, "Invalid catalog name.")
				.build()
				.writeTo(response.getOutputStream());
			return;
		}
		
	}

	@RequestMapping(value = DELETE_COLLECTION_REQUEST, method = { RequestMethod.POST })
	public void deleteCollection(
			HttpServletRequest request, 
			HttpServletResponse response, 	
			@RequestParam(value = "api", required = false) boolean api) throws IOException {

		if (apiRequested(response, api, DELETE_COLLECTION_REQUEST))
			return;
		
		com.c9a.buffers.CatalogServiceRequestProtocalBuffer.DeleteCatalogCollectionRequest deleteCollectionRequest = com.c9a.buffers.CatalogServiceRequestProtocalBuffer.DeleteCatalogCollectionRequest.parseFrom(GoogleProtoclBufferRequestContentUtils.getContentForRequest(request));
		
		try {
			catalogService.deleteCollection(deleteCollectionRequest.getUserId(), deleteCollectionRequest.getPartitionId(), deleteCollectionRequest.getCollectionUniqueId(), false);
			CatalogGPBUtils
			.generateResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.SUCCESS, "")
			.build()
			.writeTo(response.getOutputStream());
		return;
		} catch (CollectionNotFoundException e) {
			CatalogGPBUtils
			.generateResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.COLLECTION_NOT_FOUND, "The collection was not found with the give unique id : " + deleteCollectionRequest.getCollectionUniqueId())
			.build()
			.writeTo(response.getOutputStream());
			return;
		}
	}

	@RequestMapping(value = GET_REFERENCE_REQUEST, method = { RequestMethod.POST })
	public void getReference(
			HttpServletRequest request, 
			HttpServletResponse response, 	
			@RequestParam(value = "api", required = false) boolean api) throws IOException {

		if (apiRequested(response, api, GET_REFERENCE_REQUEST))
			return;
		
		com.c9a.buffers.CatalogServiceRequestProtocalBuffer.GetReferenceRequest getReferenceRequest = com.c9a.buffers.CatalogServiceRequestProtocalBuffer.GetReferenceRequest.parseFrom(GoogleProtoclBufferRequestContentUtils.getContentForRequest(request));
		
		try {
			Reference ref = catalogService.getReference(
												getReferenceRequest.getUserId(), 
												getReferenceRequest.getPartitionId(), 
												getReferenceRequest.getUniqueId());
			CatalogGPBUtils
				.generateReferenceResponseProtoBuf(ref, com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.SUCCESS, "")
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (ReferenceNotFoundException e) {
			CatalogGPBUtils
				.generateCatalogCollectionResponseProtoBufError(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.REFERENCE_NOT_FOUND, "The reference can not be found with unique id : " + getReferenceRequest.getUniqueId())
				.build()
				.writeTo(response.getOutputStream());
			return;
		}
	}

	@RequestMapping(value = ADD_DOCUMENT_REQUEST, method = { RequestMethod.POST })
	public void addDocument(
			HttpServletRequest request, 
			HttpServletResponse response, 	
			@RequestParam(value = "api", required = false) boolean api) throws IOException {
		
		if (apiRequested(response, api, ADD_DOCUMENT_REQUEST))
			return;
		
		com.c9a.buffers.CatalogServiceRequestProtocalBuffer.AddDocumentRequest addNewDocumentRequest = com.c9a.buffers.CatalogServiceRequestProtocalBuffer.AddDocumentRequest.parseFrom(GoogleProtoclBufferRequestContentUtils.getContentForRequest(request));
		
		
		try {
			
			Reference ref = catalogService.addDocument(
					addNewDocumentRequest.getUserId(), 
					addNewDocumentRequest.getPartitionId(), 
					addNewDocumentRequest.getDocument().getName(), 
					addNewDocumentRequest.getParentCatalogCollectionId(), 
					addNewDocumentRequest.getDocument().getContent().toByteArray(), 
					createPermissionListFromProtoBufEnumList(addNewDocumentRequest.getPermissionsList()), 
					createNewMap(addNewDocumentRequest.getAttributesList()));
			
			CatalogGPBUtils
				.generateReferenceResponseProtoBuf(ref, com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.SUCCESS, "")
				.build()
				.writeTo(response.getOutputStream());
			return;
			
		} catch (CollectionNotFoundException e) {
			CatalogGPBUtils
				.generateCatalogCollectionResponseProtoBufError(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.COLLECTION_NOT_FOUND, "The collection can not be found with unique id : " + addNewDocumentRequest.getParentCatalogCollectionId())
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (CollectionModificationException e) {
			CatalogGPBUtils
				.generateCatalogCollectionResponseProtoBufError(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.COLLECTION_CAN_NOT_BE_MODIFIED, "The parent collection can not be modified : " + addNewDocumentRequest.getParentCatalogCollectionId())
				.build()
				.writeTo(response.getOutputStream());
			return;
		}
	}

	@RequestMapping(value = SHARE_REFERENCE_REQUEST, method = { RequestMethod.POST })
	public void shareReference(
			HttpServletRequest request, 
			HttpServletResponse response, 	
			@RequestParam(value = "api", required = false) boolean api) throws IOException {

		if (apiRequested(response, api, SHARE_REFERENCE_REQUEST))
			return;
		

		com.c9a.buffers.CatalogServiceRequestProtocalBuffer.ShareReferenceRequest shareReferenceRequest = com.c9a.buffers.CatalogServiceRequestProtocalBuffer.ShareReferenceRequest.parseFrom(GoogleProtoclBufferRequestContentUtils.getContentForRequest(request));
		
		try {
			catalogService.shareReference(
					shareReferenceRequest.getUserId(), 
					shareReferenceRequest.getPartitionId(), 
					shareReferenceRequest.getReferenceToShareUniqueId(), 
					shareReferenceRequest.getApplicationName(), 
					createNewMap(shareReferenceRequest.getUsersAndPartitionsToShareWithList()), 
					createPermissionListFromProtoBufEnumList(shareReferenceRequest.getPermissionsList()));
			
			CatalogGPBUtils
				.generateResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.SUCCESS, "")
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (ReferenceNotFoundException e) {
			CatalogGPBUtils
				.generateCatalogCollectionResponseProtoBufError(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.REFERENCE_NOT_FOUND, "The reference can not be found with unique id : " + shareReferenceRequest.getReferenceToShareUniqueId())
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (ReferenceSharingException e) {
			CatalogGPBUtils
				.generateCatalogCollectionResponseProtoBufError(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.REFERENCE_CAN_NOT_BE_SHARED, "The reference can not be shared.")
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (InvalidCatalogNameException e) {
			CatalogGPBUtils
				.generateCatalogCollectionResponseProtoBufError(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.CATALOG_NAME_INVALID, "Invalid catalog name.")
				.build()
				.writeTo(response.getOutputStream());
			return;
		} 
	}

	@RequestMapping(value = DELETE_REFERENCE_REQUEST, method = { RequestMethod.POST })
	public void deleteReference(
			HttpServletRequest request, 
			HttpServletResponse response, 	
			@RequestParam(value = "api", required = false) boolean api) throws IOException {

		if (apiRequested(response, api, DELETE_REFERENCE_REQUEST))
			return;
		
		com.c9a.buffers.CatalogServiceRequestProtocalBuffer.DeleteReferenceRequest deleteReferenceRequest = com.c9a.buffers.CatalogServiceRequestProtocalBuffer.DeleteReferenceRequest.parseFrom(GoogleProtoclBufferRequestContentUtils.getContentForRequest(request));
		
		try {
			catalogService.deleteReferenceFromCollection(
					deleteReferenceRequest.getUserId(), 
					deleteReferenceRequest.getPartitionId(), 
					deleteReferenceRequest.getReferenceUniqueId(), false);
			CatalogGPBUtils
				.generateResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.SUCCESS, "")
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (CollectionModificationException e) {
			CatalogGPBUtils
				.generateResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.COLLECTION_CAN_NOT_BE_MODIFIED, "You do not have the needed permissions to modify the collection that holds this reference.")
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (ReferenceNotFoundException e) {
			CatalogGPBUtils
				.generateResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.REFERENCE_NOT_FOUND, "The reference can not be found with unique id : " + deleteReferenceRequest.getReferenceUniqueId())
				.build()
				.writeTo(response.getOutputStream());
			return;
		} 
	}
	
	@RequestMapping(value = MODIFY_COLLECTION_REQUEST, method = { RequestMethod.POST })
	public void modifyCollection(
			HttpServletRequest request, 
			HttpServletResponse response, 	
			@RequestParam(value = "api", required = false) boolean api) throws IOException {

		if (apiRequested(response, api, MODIFY_COLLECTION_REQUEST))
			return;
		
		com.c9a.buffers.CatalogServiceRequestProtocalBuffer.ModifyCatalogCollectionRequest modifyCollectionRequest = com.c9a.buffers.CatalogServiceRequestProtocalBuffer.ModifyCatalogCollectionRequest.parseFrom(GoogleProtoclBufferRequestContentUtils.getContentForRequest(request));
		try {
			CatalogCollection catalogCollection = catalogService.modifyCollection(
					modifyCollectionRequest.getUserId(), 
					modifyCollectionRequest.getPartitionId(), 
					modifyCollectionRequest.getCollectionUniqueId(), 
					modifyCollectionRequest.getCollectionName(), 
					modifyCollectionRequest.getParentCollectionUniqueId(), 
					createPermissionListFromProtoBufEnumList(modifyCollectionRequest.getPermissionsList()), 
					createNewMap(modifyCollectionRequest.getAttributesList()));
			CatalogGPBUtils
				.generateCatalogCollectionResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.SUCCESS, "", catalogCollection)
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (CollectionNotFoundException e) {
			CatalogGPBUtils
				.generateResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.COLLECTION_NOT_FOUND, "The collection can not be found with unique id : " + modifyCollectionRequest.getCollectionUniqueId())
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (CollectionModificationException e) {
			CatalogGPBUtils
				.generateResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.COLLECTION_CAN_NOT_BE_MODIFIED, "You do not have the needed permissions to modify this collection.")
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (InvalidCatalogNameException e) {
			CatalogGPBUtils
				.generateResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.CATALOG_NAME_INVALID, "You can not store the name \"" + modifyCollectionRequest.getCollectionName() + "\" in the catalog.")
				.build()
				.writeTo(response.getOutputStream());
			return;
		} 		
	}
	
	@RequestMapping(value = MODIFY_REFERENCE_REQUEST, method = { RequestMethod.POST })
	public void modifyREFERENCE(
			HttpServletRequest request, 
			HttpServletResponse response, 	
			@RequestParam(value = "api", required = false) boolean api) throws IOException {

		if (apiRequested(response, api, MODIFY_REFERENCE_REQUEST))
			return;
		
		com.c9a.buffers.CatalogServiceRequestProtocalBuffer.ModifyReferenceRequest modifyReferenceRequest = com.c9a.buffers.CatalogServiceRequestProtocalBuffer.ModifyReferenceRequest.parseFrom(GoogleProtoclBufferRequestContentUtils.getContentForRequest(request));
		
		try {
			Reference ref = catalogService.modifyReference(
					modifyReferenceRequest.getUserId(), 
					modifyReferenceRequest.getPartitionId(), 
					modifyReferenceRequest.getReferenceUniqueId(), 
					modifyReferenceRequest.getReferenceName(), 
					createPermissionListFromProtoBufEnumList(modifyReferenceRequest.getPermissionsList()), 
					createNewMap(modifyReferenceRequest.getAttributesList()));
			
			CatalogGPBUtils
				.generateReferenceResponseProtoBuf(ref, com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.SUCCESS, "")
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (CollectionNotFoundException e) {
			CatalogGPBUtils
				.generateResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.COLLECTION_NOT_FOUND, "The collection can not be found with unique id : " + modifyReferenceRequest.getParentCollectionUniqueId())
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (ReferenceModificationException e) {
			CatalogGPBUtils
				.generateResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.REFERENCE_CAN_NOT_BE_MODIFIED, "You do not have the needed permissions to modify this reference.")
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (ReferenceNotFoundException e) {
			CatalogGPBUtils
				.generateResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.REFERENCE_NOT_FOUND, "The reference can not be found with unique id : " + modifyReferenceRequest.getReferenceUniqueId())
				.build()
				.writeTo(response.getOutputStream());
			return;
		} catch (InvalidCatalogNameException e) {
			CatalogGPBUtils
				.generateResponseProtoBuf(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.ResponseCode.CATALOG_NAME_INVALID, "You can not store the name \"" + modifyReferenceRequest.getReferenceName() + "\" int he catalog.")
				.build()
				.writeTo(response.getOutputStream());
			return;
		} 
	}
	
	private boolean apiRequested(HttpServletResponse response, boolean api,	String request) throws IOException {
		if (api) {
			RequestResponse rr = SERVICE_SUPPORT_MAP.get(request);
			if (rr != null) {
				StringBuffer sb = new StringBuffer();
				sb.append(rr.getRequest().getDescriptorForType().getFullName()).append(" : ")
						.append(rr.getResponse().getDescriptorForType().getFullName());
				response.getOutputStream().write(sb.toString().getBytes());
			} else {
				response.getOutputStream().write(
						"No matching service for url".getBytes());
			}
			return true;
		}
		return false;
	}
	
	private Map<String, String> createNewMap(List<com.c9a.buffers.CatalogServiceResponseProtocalBuffer.KeyValuePair> kvpList) {
		if(kvpList != null && kvpList.size() > 0){
			Map<String, String> attrs = new HashMap<String, String>();
			for(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.KeyValuePair kvp : kvpList){
				attrs.put(kvp.getKey(), kvp.getValue());
			}
			return attrs;
		}
		return null;
	}

	private Set<Long> createPermissionListFromProtoBufEnumList(List<com.c9a.buffers.CatalogServiceResponseProtocalBuffer.Permission> permissions) {
		Set<Long> permissionIds = new HashSet<Long>();
		for(com.c9a.buffers.CatalogServiceResponseProtocalBuffer.Permission p : permissions){
			permissionIds.add(Long.valueOf(p.getNumber()));
		}
		return permissionIds;
	}
}