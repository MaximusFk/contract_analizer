package com.contractanalizer.contracts;

import org.web3j.protocol.core.methods.response.EthBlock.Block;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import com.contractanalizer.contracts.ERC20.TransferEventResponse;

public class TransferEvent {
	
	private TransactionReceipt transactionReceipt;
	private TransferEventResponse transferEventResponse;
	private Block block;
	
	public TransferEvent(TransactionReceipt transactionReceipt, TransferEventResponse transferEventResponse, Block block) {
		this.transactionReceipt = transactionReceipt;
		this.transferEventResponse = transferEventResponse;
		this.block = block;
	}
	
	public TransactionReceipt getTransactionReceipt() {
		return transactionReceipt;
	}
	
	public void setTransactionReceipt(TransactionReceipt transactionReceipt) {
		this.transactionReceipt = transactionReceipt;
	}
	
	public TransferEventResponse getTransferEventResponse() {
		return transferEventResponse;
	}
	
	public void setTransferEventResponse(TransferEventResponse transferEventResponse) {
		this.transferEventResponse = transferEventResponse;
	}

	public Block getBlock() {
		return block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}
	

}
