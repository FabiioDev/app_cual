package com.example.cualma;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class AdminSQLiteOpenHelper extends SQLiteOpenHelper {

    public AdminSQLiteOpenHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tabla Estudiantes
        db.execSQL("CREATE TABLE estudiantes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "codigo_estudiante TEXT, " +
                "nombres TEXT, " +
                "apellidos TEXT, " +
                "carnet TEXT, " +
                "carrera TEXT)");

        // Tabla Docentes (Como referencia simple según requerimiento)
        db.execSQL("CREATE TABLE docentes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre_completo TEXT)");

        // Tabla Materias (Incluye id_estudiante como clave foránea lógica)
        db.execSQL("CREATE TABLE materias (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "codigo_materia TEXT, " +
                "nombre TEXT, " +
                "hora_inicio TEXT, " +
                "hora_fin TEXT, " +
                "aula TEXT, " +
                "nombre_docente TEXT, " +
                "id_estudiante INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS estudiantes");
        db.execSQL("DROP TABLE IF EXISTS docentes");
        db.execSQL("DROP TABLE IF EXISTS materias");
        onCreate(db);
    }
}