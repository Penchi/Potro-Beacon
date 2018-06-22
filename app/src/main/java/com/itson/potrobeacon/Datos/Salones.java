package com.itson.potrobeacon.Datos;

import java.util.Map;

/**
 * Created by orlan on 05/03/2018.
 */

public class Salones
{
    private Map<String,Object> salones;

    public Salones()
    {

    }

    public Salones(Map<String, Object> salones) {
        this.salones = salones;
    }

    public Map<String, Object> getSalones() {
        return salones;
    }

    public void setSalones(Map<String, Object> salones) {
        this.salones = salones;
    }
}
