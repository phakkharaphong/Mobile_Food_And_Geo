package com.example.work_2_6430206321.Util

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreRepository {
    private val firestoreDB: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    fun getSavedFoodDataFromOnlineDB(): CollectionReference {
        var collectionReference = firestoreDB.collection("House")
        return collectionReference
    }

}
