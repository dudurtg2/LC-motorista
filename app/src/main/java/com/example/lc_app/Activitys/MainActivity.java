package com.example.lc_app.Activitys;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.lc_app.Activitys.User.Controler.WorkHourActivity;
import com.example.lc_app.Activitys.User.ProfileActivity;
import com.example.lc_app.R;
import com.example.lc_app.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public ActivityMainBinding binding;
    private DocumentReference docRef, docRefRTA;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private String rota, rtaRota;
    private boolean remove;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        remove = false;
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getUser();

        binding.qrCodeImageView.setOnClickListener(v-> {
            remove = false;
            IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
            integrator.setCaptureActivity(CaptureActivity.class);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });
        binding.buttonList.setOnClickListener(v-> {
            remove = true;
            IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
            integrator.setCaptureActivity(CaptureActivity.class);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });
        binding.buttonProfileUser.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        binding.buttonWorkHour.setOnClickListener(v -> startActivity(new Intent(this, WorkHourActivity.class)));
    }

    private void confirmDocExist(String uid) {
        if (mAuth.getCurrentUser() != null) {
            docRef = firestore.collection("bipagem").document(uid);
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    if (remove) {removeToTraver(uid);}
                    else { addToTraver(uid);}
                } else { Toast.makeText(this, "RTA não encontrado", Toast.LENGTH_SHORT).show(); }
            }).addOnFailureListener(e -> Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());
        } else { Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show(); }
    }

    private void addToTraver(String uid) {
        docRefRTA = firestore.collection("bipagem").document(uid);
        docRefRTA.get().addOnSuccessListener(documentSnapshotRTA -> {
            if (documentSnapshotRTA.exists()) {
                if (documentSnapshotRTA.getString("Status").equals("aguardando")) {
                    rtaRota = documentSnapshotRTA.getString("Rota");
                    firestore.collection("usuarios").document(mAuth.getCurrentUser().getUid())
                            .get().addOnSuccessListener(documentSnapshotUsuario -> {
                                if (documentSnapshotUsuario.exists()) {
                                    rota = documentSnapshotUsuario.getString("rota");
                                    if (rota.equals(rtaRota)) {
                                        docRefRTA.update("Motorista", mAuth.getCurrentUser().getUid())
                                                .addOnSuccessListener(aVoid -> {
                                                    firestore.collection("usuarios").document(mAuth.getCurrentUser().getUid())
                                                            .update("RTA_sacas", FieldValue.arrayUnion(uid))
                                                            .addOnSuccessListener(v ->
                                                                docRefRTA.update("Status", "em rota")
                                                                        .addOnSuccessListener(aVoid2 -> Toast.makeText(this, "RTA " + uid + " adicionada a rota.", Toast.LENGTH_SHORT).show()))
                                                            .addOnFailureListener(e -> Toast.makeText(this, "Erro ao adicionar RTA a rota. " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(this, "Erro ao atualizar status." + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    } else {Toast.makeText(this, "Rota da RTA não corresponde a rota do motorista", Toast.LENGTH_SHORT).show();}
                                }
                            });
                } else if (documentSnapshotRTA.getString("Status").equals("em rota")) { Toast.makeText(this, "Motorista já está em rota", Toast.LENGTH_SHORT).show();}
                else if (documentSnapshotRTA.getString("Status").equals("finalizado")) {Toast.makeText(this, "Motorista já finalizou", Toast.LENGTH_SHORT).show();}
            }
        });
    }

    private void removeToTraver(String uid) {
        docRefRTA = firestore.collection("bipagem").document(uid);
        docRefRTA.get().addOnSuccessListener(documentSnapshotRTA -> {
            if (documentSnapshotRTA.exists()) {
                String motorista = documentSnapshotRTA.getString("Motorista");
                String status = documentSnapshotRTA.getString("Status");
                if (motorista != null && motorista.equals(mAuth.getCurrentUser().getUid()) && status != null && status.equals("em rota")) {
                    firestore.collection("usuarios").document(mAuth.getCurrentUser().getUid())
                            .get().addOnSuccessListener(documentSnapshotUsuario -> {
                                if (documentSnapshotUsuario.exists()) {
                                    docRefRTA.update("Status", "finalizado").addOnSuccessListener(aVoid -> {
                                        docRefRTA.update("Fim", FieldValue.serverTimestamp());
                                        //deleteRTAFirebase(uid);
                                        firestore.collection("usuarios").document(mAuth.getCurrentUser().getUid())
                                                .update("RTA_sacas", FieldValue.arrayRemove(uid))
                                                .addOnSuccessListener(aVoid2 -> Toast.makeText(this, "RTA " + uid + " finalizada.", Toast.LENGTH_SHORT).show())
                                                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao remover RTA da rota: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                    }).addOnFailureListener(e -> Toast.makeText(this, "Erro ao atualizar status." + e.getMessage(), Toast.LENGTH_SHORT).show());
                                }
                            });
                } else { Toast.makeText(this, "Motorista não corresponde a RTA", Toast.LENGTH_SHORT).show(); }
            }
        });
    }

    /*
    private void deleteRTAFirebase(String uid){
        docRefRTA = firestore.collection("bipagem").document(uid);
        docRefRTA.get().addOnSuccessListener(documentSnapshotRTA -> {
            if (documentSnapshotRTA.exists()) {
                docRefRTA.delete().addOnSuccessListener(
                        aVoid -> Toast.makeText(this, "RTA " + uid + " removida.", Toast.LENGTH_SHORT).show()
                        ).addOnFailureListener(e -> Toast.makeText(this, "Erro ao remover RTA: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
    */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) { Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();}
            else {confirmDocExist(result.getContents());}
        } else { super.onActivityResult(requestCode, resultCode, data);}
    }

    private void getUser() {
        firestore.collection("usuarios").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) { binding.UserNameDisplay.setText(documentSnapshot.getString("nome")); }
                    else { binding.UserNameDisplay.setText(mAuth.getCurrentUser().getDisplayName()); }
                })
                .addOnFailureListener(e ->  Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());
    }
}
