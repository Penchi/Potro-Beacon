package com.itson.potrobeacon.Datos;

import java.util.List;
import java.util.Map;

/**
 * Created by orlan on 04/03/2018.
 */

public class Salon {

    private int id;
    private Map<String,Object> clases;

    public Salon()
    {

    }

    public Salon(int id, Map<String, Object> clases) {
        this.id = id;
        this.clases = clases;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Map<String, Object> getClases() {
        return clases;
    }

    public void setClases(Map<String, Object> clases) {
        this.clases = clases;
    }
}
