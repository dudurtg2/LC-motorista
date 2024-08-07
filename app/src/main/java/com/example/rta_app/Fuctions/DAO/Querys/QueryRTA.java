package com.example.rta_app.Fuctions.DAO.Querys;

import android.content.Context;
import android.widget.Toast;
import com.example.rta_app.Fuctions.DTO.ListRTADTO;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class QueryRTA {
  private Context context;
  private FirebaseFirestore db;
  private List<ListRTADTO> list;
  private FirebaseAuth mAuth;

  public QueryRTA(Context context) {
    this.context = context;
    this.db = FirebaseFirestore.getInstance();
    this.list = new ArrayList<>();
    mAuth = FirebaseAuth.getInstance();
  }

  public interface FirestoreCallback { void onCallback(List<ListRTADTO> listRTADTO); }

  public void readData(final FirestoreCallback firestoreCallback) {
    db.collection("direcionado").document(mAuth.getCurrentUser().getUid()).collection("pacotes").get().addOnCompleteListener(task -> {
      if (task.isSuccessful()) {
        list.clear();
        for (QueryDocumentSnapshot document : task.getResult()) {
          String codigoDeFicha = document.getString("Codigo_de_ficha");
          String status = document.getString("Status");
          ListRTADTO listRTADTO = new ListRTADTO(codigoDeFicha, status);
          list.add(listRTADTO);
        }
        if (list.isEmpty()) {
          ListRTADTO listRTADTO = new ListRTADTO("Não a RTA no momento", "Indisponível");
          list.add(listRTADTO);
        }
        firestoreCallback.onCallback(list);
      } else {
        Toast.makeText(context, "Erro ao obter documentos: " + task.getException(), Toast.LENGTH_SHORT).show();
      }
    });
  }
  public void readDataInTravel(final FirestoreCallback firestoreCallback) {
    db.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").get().addOnCompleteListener(task -> {
      if (task.isSuccessful()) {
        list.clear();

        for (QueryDocumentSnapshot document : task.getResult()) {
          String codigoDeFicha = document.getString("Codigo_de_ficha");
          String status = document.getString("Status");
          ListRTADTO listRTADTO = new ListRTADTO(codigoDeFicha, status);
          list.add(listRTADTO);
        }
        if (list.isEmpty()) {
          ListRTADTO listRTADTO = new ListRTADTO("Não a RTA no momento", "Indisponível");
          list.add(listRTADTO);
        }
        firestoreCallback.onCallback(list);
      } else {
        Toast.makeText(context, "Erro ao obter documentos: " + task.getException(), Toast.LENGTH_SHORT).show();
      }
    });
  }
}
