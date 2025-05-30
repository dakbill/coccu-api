package com.coc.cu.utils

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials

class GoogleSheetUtils {

    private val applicationName = "Google Sheets API Kotlin"
    private val jsonFactory = GsonFactory.getDefaultInstance()

    /**
     * Creates Sheets service object with credentials
     */
    private fun getSheetsService(credentials: GoogleCredentials): Sheets {
        val credentials = credentials.createScoped(listOf(SheetsScopes.SPREADSHEETS_READONLY))

        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()

        return Sheets.Builder(httpTransport, jsonFactory, HttpCredentialsAdapter(credentials))
            .setApplicationName(applicationName)
            .build()
    }

    /**
     * Reads data from a Google Sheet
     * @param credentialsPath Path to service account JSON file
     * @param spreadsheetId The ID of the spreadsheet
     * @param range The range to read (e.g., "Sheet1!A1:E10")
     * @return List of rows, each row is a list of values
     */
    fun readSheet(
        credentials: GoogleCredentials,
        spreadsheetId: String,
        range: String
    ): List<List<Any>> {
        return try {
            val service = getSheetsService(credentials)

            val response = service.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute()

            response.getValues() ?: emptyList()
        } catch (e: Exception) {
            println("Error reading sheet: ${e.message}")
            emptyList()
        }
    }

    /**
     * Reads entire sheet by sheet name
     */
    fun readEntireSheet(
        credentials: GoogleCredentials,
        spreadsheetId: String,
        sheetName: String = "Sheet1"
    ): List<List<Any>> {
        return readSheet(credentials, spreadsheetId, "$sheetName!A:Z")
    }

    /**
     * Reads sheet data and converts to map with headers
     */
    fun readSheetAsMap(
        credentials: GoogleCredentials,
        spreadsheetId: String,
        range: String
    ): List<Map<String, Any>> {
        val data = readSheet(credentials, spreadsheetId, range)

        if (data.isEmpty()) return emptyList()

        val headers = data.first().map { it.toString() }
        val rows = data.drop(1)

        return rows.map { row ->
            headers.mapIndexed { index, header ->
                header to (row.getOrNull(index) ?: "")
            }.toMap()
        }
    }
}