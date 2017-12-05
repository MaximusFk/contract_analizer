package com.contractanalizer.xls;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Logger;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.bson.Document;

import com.mongodb.client.MongoCollection;

public final class XLSWriter implements Closeable {
	
	private static final Logger log = Logger.getLogger(XLSWriter.class.getName());
	
	private OutputStream out;
	private Workbook workbook;
	
	public XLSWriter(OutputStream out) {
		this.out = out;
		this.workbook = new SXSSFWorkbook();
	}
	
	public void writeMongoCollection(MongoCollection<Document> collection) {
		Sheet sheet = workbook.createSheet(collection.getNamespace().getCollectionName());
		int rownum = 0;
		for(Document document : collection.find()) {
			if(rownum == 0) {
				Row row = sheet.createRow(rownum);
				document.keySet().forEach(key -> {
					row.createCell(row.getLastCellNum() < 0 ? 0 : row.getLastCellNum()).setCellValue(key);
				});
				++rownum;
			}
			Row row = sheet.createRow(rownum);
			document.values().forEach(value -> {
				if(value instanceof Number) {
					Number number = (Number) value;
					row.createCell(row.getLastCellNum() < 0 ? 0 : row.getLastCellNum()).setCellValue(number.doubleValue());
				}
				else {
					row.createCell(row.getLastCellNum() < 0 ? 0 : row.getLastCellNum()).setCellValue(value.toString());
				}
				//sheet.autoSizeColumn(row.getLastCellNum() - 1);
			});
			++rownum;
		}
	}
	
	public void writeDocumentList(String sheetname, List<Document> documents) {
		documents.forEach(document -> writeBsonDocument(sheetname, document));
	}
	
	public void writeStringList(String sheetname, List<String> strings) {
		strings.forEach(string -> writeString(sheetname, string));
	}
	
	public void writeStrings(String sheetname, Iterable<String> strings) {
		strings.forEach(string -> writeString(sheetname, string));
	}
	
	public void writeBsonDocument(String sheetName, Document document) {
		Sheet sheet = workbook.getSheet(sheetName) != null ? workbook.getSheet(sheetName) : workbook.createSheet(sheetName);
		int rownum = sheet.getLastRowNum();
		if(rownum == 0) {
			Row row = sheet.createRow(rownum);
			document.keySet().forEach(key -> {
				row.createCell(row.getLastCellNum() < 0 ? 0 : row.getLastCellNum()).setCellValue(key);
			});
		}
		++rownum;
		Row row = sheet.createRow(rownum);
		document.values().forEach(value -> {
			if(value instanceof Number) {
				Number number = (Number) value;
				row.createCell(row.getLastCellNum() < 0 ? 0 : row.getLastCellNum()).setCellValue(number.doubleValue());
			}
			else {
				row.createCell(row.getLastCellNum() < 0 ? 0 : row.getLastCellNum()).setCellValue(value.toString());
			}
			//sheet.autoSizeColumn(row.getLastCellNum() - 1);
		});
	}
	
	public void writeString(String sheetName, String value) {
		Sheet sheet = workbook.getSheet(sheetName) != null ? workbook.getSheet(sheetName) : workbook.createSheet(sheetName);
		int rownum = sheet.getLastRowNum();
		Row row = sheet.createRow(rownum + 1);
		row.createCell(0).setCellValue(value);
		//sheet.autoSizeColumn(0);
	}
	
	public void flush() throws IOException {
		workbook.write(out);
	}

	@Override
	public void close() throws IOException {
		workbook.write(out);
		workbook.close();
	}
}
