package com.itson.potrobeacon.Datos;

import java.util.List;
import java.util.Map;

/**
 * Created by orlan on 04/03/2018.
 */

public class Clase {

    private int id;
    private String nombre;
    private int entrada;
    private int salida;
    private String spreadSheetId;
    private Map<String,String> alumnos;

    public Clase()
    {

    }

    public Clase(int id, String nombre, int entrada, int salida, String spreadSheetId, Map<String, String> alumnos) {
        this.id = id;
        this.nombre = nombre;
        this.entrada = entrada;
        this.salida = salida;
        this.spreadSheetId = spreadSheetId;
        this.alumnos = alumnos;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getEntrada() {
        return entrada;
    }

    public void setEntrada(int entrada) {
        this.entrada = entrada;
    }

    public int getSalida() {
        return salida;
    }

    public void setSalida(int salida) {
        this.salida = salida;
    }

    public String getSpreadSheetId() {
        return spreadSheetId;
    }

    public void setSpreadSheetId(String spreadSheetId) {
        this.spreadSheetId = spreadSheetId;
    }

    public Map<String, String> getAlumnos() {
        return alumnos;
    }

    public void setAlumnos(Map<String, String> alumnos) {
        this.alumnos = alumnos;
    }
}
