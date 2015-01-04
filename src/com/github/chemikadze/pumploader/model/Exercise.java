package com.github.chemikadze.pumploader.model;

import java.io.Serializable;
import java.util.ArrayList;

public final class Exercise implements Serializable {

    private String name;
    private ArrayList<ExerciseSet> sets;

    public Exercise(String name, ArrayList<ExerciseSet> sets) {
        this.name = name;
        this.sets = sets;
    }

    public String getName() {
        return name;
    }

    public ArrayList<ExerciseSet> getSets() {
        return sets;
    }


}
