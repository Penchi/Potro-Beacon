//Código creado por Aarón Angulo

package com.itson.potrobeacon;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;

public class RegistrarUsuario extends AppCompatActivity {

    private EditText txtId;
    private EditText txtNombre;
    private Switch maestro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar_usuario);

        txtId = (EditText) findViewById(R.id.txtID);
        txtNombre = (EditText) findViewById(R.id.txtNombre);
        maestro = (Switch) findViewById(R.id.switchMaestro);

        txtId.setText(getSharedPreferences("PotroBeacon", 0).getString("id", "").toString());
        txtNombre.setText(getSharedPreferences("PotroBeacon", 0).getString("nombre", "").toString());
        maestro.setChecked(getSharedPreferences("PotroBeacon", 0).getBoolean("maestro", false));
    }

    public void Registrar(View v)
    {
        SharedPreferences sp = getSharedPreferences("PotroBeacon", 0);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("id", txtId.getText().toString());
        editor.putString("nombre", txtNombre.getText().toString());
        editor.putBoolean("maestro", maestro.isChecked());
        editor.commit();

        finish();
    }
}
