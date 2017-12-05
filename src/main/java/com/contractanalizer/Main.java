package com.contractanalizer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.map.HashedMap;
import org.bson.Document;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.EthBlock.TransactionObject;
import org.web3j.protocol.http.HttpService;

import com.contractanalizer.contracts.ERC20;
import com.contractanalizer.contracts.TransferEvent;
import com.contractanalizer.mongodb.TransferEventDao;
import com.contractanalizer.mongodb.TransferEventDao.DocumentField;
import com.contractanalizer.xls.XLSWriter;

public class Main {

	private static final String XLS_FILE_NAME = "eox.xlsx";
	private static final String INFURA_MAIN_ACCESS = "https://mainnet.infura.io/4znnJ2a65m3bVtUSv5wb";
	private static final String EOS_CONTRACT_ADDRESS = "0x86fa049857e0209aa7d9e616f7eb3b3b78ecfdb0";
	private static final long EOS_EARLIEST_BLOCK_NUM = 3904469L; // Jun-20-2017 06:41:20 PM +UTC
	private static final long EOS_SUMMER_LATEST_BLOCK_NUM = 4225034L; // Aug-31-2017 11:58:56 PM +UTC
	private static final long SUMMER_LAST_BLOCK_NUM = 4225037L; // Aug-31-2017 11:59:56 PM +UTC

	private static final long SEPTEMBER_FIRST_BLOCK = 4225038L;
	private static final long SEPTEMBER_LAST_BLOCK = 4326060L;

	private static Web3j web3j;
	private static ERC20 contract;
	private static long count = 0;
	private static TransferEventDao transferEventDao;

	public static void main(String[] args) {
		web3j = Web3j.build(new HttpService(INFURA_MAIN_ACCESS));
		transferEventDao = new TransferEventDao();
		contract = ERC20.load(EOS_CONTRACT_ADDRESS, web3j, Credentials.create("0x00"), ERC20.GAS_PRICE,
				ERC20.GAS_LIMIT);
		web3j.ethBlockNumber().sendAsync().thenAccept(blockNumber -> {
			System.out.println("Current block heigth: " + blockNumber.getBlockNumber().toString());
		});

		final long startBlock = transferEventDao.findMaxBlockNumber(SEPTEMBER_FIRST_BLOCK);
		final long endBlock = SEPTEMBER_LAST_BLOCK;// startBlock + 50; //

		startTransactionSync(startBlock, endBlock);
	}

	private static void startTransactionSync(long startBlock, long endBlock) throws IllegalArgumentException {
		if (startBlock > endBlock) {
			throw new IllegalArgumentException("endBlock must be larger startBlock");
		}
		System.out.println("Starting tx replay from: " + startBlock + " to: " + endBlock);
		web3j.replayBlocksObservable(new DefaultBlockParameterNumber(startBlock),
				new DefaultBlockParameterNumber(endBlock), true).doOnCompleted(Main::analizeData)
				.doOnTerminate(Main::exit).doOnError(Main::printError).subscribe(block -> {
					block.getBlock().getTransactions().stream().map(tx -> TransactionObject.class.cast(tx))
							.filter(tx -> EOS_CONTRACT_ADDRESS.equals(tx.getTo())).forEach(tx -> {
								++count;
								if ((count % 100) == 0) {
									System.out.print("Iteration:" + count);
									System.out.print(" Block(" + tx.getBlockNumber().toString() + ")");
									System.out.println(
											" Remaining " + (endBlock - tx.getBlockNumber().longValue()) + " blocks");
								}
								web3j.ethGetTransactionReceipt(tx.getHash()).sendAsync().thenAccept(transaction -> {
									transaction.getResult().setContractAddress(EOS_CONTRACT_ADDRESS);
									contract.getTransferEvents(transaction.getResult()).forEach(transfer -> {
										transferEventDao.add(
												new TransferEvent(transaction.getResult(), transfer, block.getBlock()));
									});
								});
							});
				});
	}

	private static void analizeData() {
		System.out.println("Writer is running");
		try (XLSWriter writer = new XLSWriter(new FileOutputStream(XLS_FILE_NAME))) {
			writer.writeMongoCollection(transferEventDao.getCollection());
			writer.writeDocumentList("TransactionsStats", analizeTransactionForDate());
			writer.writeStrings("TransactionSenders",
					transferEventDao.getDistinctString(TransferEventDao.DocumentField.SENDER));
			writer.writeStrings("TransactionReceivers",
					transferEventDao.getDistinctString(TransferEventDao.DocumentField.RECEIVER));
			Set<String> holders = new HashSet<String>();
			transferEventDao.getDistinctString(TransferEventDao.DocumentField.SENDER).iterator().forEachRemaining(holders::add);
			transferEventDao.getDistinctString(TransferEventDao.DocumentField.RECEIVER).iterator().forEachRemaining(holders::add);
			writer.writeStrings("TokenHolders", holders);
			writer.writeBsonDocument("TotalStats", analizeTotalStats());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static List<Document> analizeTransactionForDate() {
		Map<String, Document> transactions = new HashedMap<>();
		transferEventDao.getAll().iterator().forEachRemaining(document -> {
			String blockDate = new SimpleDateFormat("dd/MM/yyyy")
					.format(document.getLong(TransferEventDao.DocumentField.TIMESTAMP.getFieldName()) * 1000);
			if (!transactions.containsKey(blockDate)) {
				transactions.put(blockDate, new Document()
						.append("Date", blockDate)
						.append("Transactions", 1)
						.append("TokensTransfer", document.getDouble(TransferEventDao.DocumentField.VALUE.getFieldName())));
			} else {
				Document current = transactions.get(blockDate);
				current.replace("Transactions", current.getInteger("Transactions") + 1);
				current.replace("TokensTransfer", current.getDouble("TokensTransfer") + document.getDouble(TransferEventDao.DocumentField.VALUE.getFieldName()));
			}
		});
		return new ArrayList<>(transactions.values());
	}
	
	private static Document analizeTotalStats() {
		long firstBlock = transferEventDao.findMinBlockNumber(SEPTEMBER_FIRST_BLOCK);
		long lastBlock = transferEventDao.findMaxBlockNumber(SEPTEMBER_LAST_BLOCK);
		long transactionCount = transferEventDao.getCollection().count();
		double transPerBlock = transactionCount / (lastBlock - firstBlock);
		double minTransValue = transferEventDao.findMinValue();
		double maxTransValue = transferEventDao.findMaxValue();
		return new Document()
				   .append("FirstBlock", firstBlock)
				   .append("LastBlock", lastBlock)
				   .append("TransferCount", transactionCount)
				   .append("TransfersPerBlock", transPerBlock)
				   .append("MinimumSendedTokens", minTransValue)
				   .append("MaximumSendedTokens", maxTransValue);
	}

	private static void printError(Throwable error) {
		System.err.println(error.getMessage());
	}

	private static void exit() {
		Runtime.getRuntime().exit(0);
	}

}
