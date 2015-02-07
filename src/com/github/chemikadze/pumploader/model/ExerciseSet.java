package com.github.chemikadze.pumploader.model;

import java.io.Serializable;
import java.util.ArrayList;

public final class ExerciseSet implements Serializable {

    private String name;
    private ArrayList<ExerciseReps> reps;

    public ExerciseSet(String name, ArrayList<ExerciseReps> reps) {
        this.name = name;
        this.reps = reps;
    }

    public String getName() {
        return name;
    }

    public ArrayList<ExerciseReps> getReps() {
        return reps;
    }


}
