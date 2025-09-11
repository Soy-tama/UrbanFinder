package com.example.urbanfinder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.UnknownHostException

@Composable
fun NameListPage(navController: NavController) {
    var names by remember { mutableStateOf<List<String>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Fetch data when the page loads
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val fetchedNames = fetchNamesFromServer()
                names = fetchedNames
            } catch (e: UnknownHostException) {
                errorMessage = "Unable to connect to the server. Check your network."
            } catch (e: Exception) {
                errorMessage = "An error occurred: ${e.localizedMessage}"
            }
        }
    }

    // Display data
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (errorMessage != null) {
            Text(text = errorMessage ?: "Unknown error", modifier = Modifier.padding(16.dp))
        } else if (names.isEmpty()) {
            Text(text = "Loading...", modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(names) { name ->
                    Text(
                        text = name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

suspend fun fetchNamesFromServer(): List<String> {
    // Creating an OkHttpClient
    val client = OkHttpClient()

    // Making a request to the Flask server
    val request = Request.Builder()
        .url("http://192.168.1.73:5000/name") // Replace with your Flask server IP
        .build()

    // Execute the request synchronously
    val response = client.newCall(request).execute()
    val responseBody = response.body?.string() ?: throw Exception("No response from server")

    // Parse the response body into a JSONArray
    val jsonArray = JSONArray(responseBody)
    val namesList = mutableListOf<String>()

    // Extract "name" from each item in the JSON array
    for (i in 0 until jsonArray.length()) {
        val jsonObject = jsonArray.getJSONObject(i)
        val name = jsonObject.getString("name")
        namesList.add(name)
    }

    return namesList
}
