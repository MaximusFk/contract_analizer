package com.contractanalizer.mongodb;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.contractanalizer.config.MongoDBConfig;
import com.contractanalizer.contracts.ERC20;
import com.contractanalizer.contracts.TransferEvent;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;

public class TransferEventDao {
	
	public static final String COLLECTION_NAME = "ERC20Transfers";
	private MongoClient mongoClient;
	
	public TransferEventDao() {
		MongoCredential credential = MongoCredential.createCredential(MongoDBConfig.USER, MongoDBConfig.DATABASE, MongoDBConfig.PASSWORD);
		ServerAddress address = new ServerAddress(MongoDBConfig.HOST, MongoDBConfig.PORT);
		this.mongoClient = new MongoClient(address);
		MongoCollection<Document> collection = loadCollection();
		collection.createIndex(new Document(DocumentField.TRANSACTION_HASH.getFieldName(), 1), new IndexOptions().unique(true));
	}
	
	public void add(TransferEvent event) {
		MongoCollection<Document> collection = loadCollection();
		Document eventDocument = new Document();
		eventDocument.append(DocumentField.BLOCK_NUM.getFieldName(), event.getTransactionReceipt().getBlockNumber().longValue())
					 .append(DocumentField.TRANSACTION_HASH.getFieldName(), event.getTransactionReceipt().getTransactionHash())
					 .append(DocumentField.CONTRACT_ADDRESS.getFieldName(), event.getTransactionReceipt().getContractAddress())
					 .append(DocumentField.SENDER.getFieldName(), event.getTransferEventResponse().from)
					 .append(DocumentField.RECEIVER.getFieldName(), event.getTransferEventResponse().to)
					 .append(DocumentField.VALUE.getFieldName(), event.getTransferEventResponse().value.doubleValue() / Math.pow(10.f, ERC20.DEFAULT_DECIMALS))
					 .append(DocumentField.TIMESTAMP.getFieldName(), event.getBlock().getTimestamp().longValue());
		collection.insertOne(eventDocument);
	}
	
	public Document getById(String id) {
		MongoCollection<Document> collection = loadCollection();
		Document document = collection.find(Filters.eq(DocumentField.ID.getFieldName(), new ObjectId(id))).limit(1).first();
		return document;
	}
	
	public <T> Document findBy(DocumentField field, T value) {
		MongoCollection<Document> collection = loadCollection();
		Document document = collection.find(Filters.eq(field.getFieldName(), value)).limit(1).first();
		return document;
	}
	
	public void update(Document document) {
		MongoCollection<Document> collection = loadCollection();
		collection.updateOne(Filters.eq(DocumentField.ID.getFieldName(), document.get(DocumentField.ID.getFieldName())), document);
	}
	
	public MongoCollection<Document> getCollection() {
		return loadCollection();
	}
	
	public FindIterable<Document> getAll() {
		MongoCollection<Document> collection = loadCollection();
		return collection.find();
	}
	
	public double findMaxValue() {
		MongoCollection<Document> collection = loadCollection();
		if(collection.count() > 0) {
			Document document = collection.find().sort(new Document(DocumentField.VALUE.getFieldName(), -1)).limit(1).first();
			return document.getDouble(DocumentField.VALUE.getFieldName());
		}
		else {
			return 0.f;
		}
	}
	
	public double findMinValue() {
		MongoCollection<Document> collection = loadCollection();
		if(collection.count() > 0) {
			Document document = collection.find().sort(new Document(DocumentField.VALUE.getFieldName(), 1)).limit(1).first();
			return document.getDouble(DocumentField.VALUE.getFieldName());
		}
		else {
			return 0.f;
		}
	}
	
	public long findMaxBlockNumber(long defaultValue) {
		MongoCollection<Document> collection = loadCollection();
		if(collection.count() > 0) {
			Document document = collection.find().sort(new Document(DocumentField.BLOCK_NUM.getFieldName(), -1)).limit(1).first();
			return document.getLong(DocumentField.BLOCK_NUM.getFieldName());
		}
		else {
			return defaultValue;
		}
	}
	
	public long findMinBlockNumber(long defaultValue) {
		MongoCollection<Document> collection = loadCollection();
		if(collection.count() > 0) {
			Document document = collection.find().sort(new Document(DocumentField.BLOCK_NUM.getFieldName(), 1)).limit(1).first();
			return document.getLong(DocumentField.BLOCK_NUM.getFieldName());
		}
		else {
			return defaultValue;
		}
	}
	
	public DistinctIterable<String> getDistinctString(DocumentField field) {
		MongoCollection<Document> collection = loadCollection();
		return collection.distinct(field.getFieldName(), String.class);
	}
	
	private MongoCollection<Document> loadCollection() {
		MongoDatabase database = mongoClient.getDatabase(MongoDBConfig.DATABASE);
		return database.getCollection(COLLECTION_NAME);
	}
	
	public enum DocumentField {
		
		ID("_id"),
		VALUE("value"),
		RECEIVER("to"),
		SENDER("from"),
		CONTRACT_ADDRESS("contract_address"),
		TRANSACTION_HASH("tx_hash"),
		BLOCK_NUM("block"),
		TIMESTAMP("timestamp");
		
		private String fieldName;
		
		private DocumentField(String fieldName) {
			this.fieldName = fieldName;
		}
		
		public String getFieldName() {
			return this.fieldName;
		}
	}
	
}
