package com.example.cualma;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ActivityFormSubject extends AppCompatActivity {

    private EditText codeEt, nameEt, startEt, endEt, classroomEt, teacherEt;
    private Button saveButton;
    private FloatingActionButton deleteButton;
    private ImageButton backButton;
    private TextView titleTextView;

    private int subjectId = -1;
    private int studentId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_form_subject);

        initializeViews();

        if (getIntent().hasExtra("STUDENT_ID")) {
            studentId = getIntent().getIntExtra("STUDENT_ID", -1);
        }

        if (getIntent().hasExtra("SUBJECT_ID")) {
            subjectId = getIntent().getIntExtra("SUBJECT_ID", -1);
            titleTextView.setText(R.string.form_subject_edit_title);
            deleteButton.setVisibility(View.VISIBLE);
            loadSubjectData();
        }

        setupListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        codeEt = findViewById(R.id.subjectCodeEditText);
        nameEt = findViewById(R.id.subjectNameEditText);
        startEt = findViewById(R.id.scheduleStartEditText);
        endEt = findViewById(R.id.scheduleEndEditText);
        classroomEt = findViewById(R.id.classroomEditText);
        teacherEt = findViewById(R.id.teacherNameEditText);

        saveButton = findViewById(R.id.saveButton);
        deleteButton = findViewById(R.id.deleteButton);
        backButton = findViewById(R.id.backButton);
        titleTextView = findViewById(R.id.titleTextView);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> validateAndSave());
        deleteButton.setOnClickListener(v -> confirmDelete());
    }

    private void loadSubjectData() {
        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this, "bd_cualma", null, 1);
        SQLiteDatabase db = admin.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT codigo_materia, nombre, hora_inicio, hora_fin, aula, nombre_docente FROM materias WHERE id = ?", new String[]{String.valueOf(subjectId)});

        if (cursor.moveToFirst()) {
            codeEt.setText(cursor.getString(0));
            nameEt.setText(cursor.getString(1));
            startEt.setText(cursor.getString(2));
            endEt.setText(cursor.getString(3));
            classroomEt.setText(cursor.getString(4));
            teacherEt.setText(cursor.getString(5));
        }
        cursor.close();
        db.close();
    }

    private void validateAndSave() {
        String code = codeEt.getText().toString().trim();
        String name = nameEt.getText().toString().trim();
        String start = startEt.getText().toString().trim();
        String end = endEt.getText().toString().trim();
        String classroom = classroomEt.getText().toString().trim();
        String teacher = teacherEt.getText().toString().trim();

        if (code.isEmpty() || name.isEmpty() || start.isEmpty() || end.isEmpty() || classroom.isEmpty() || teacher.isEmpty()) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this, "bd_cualma", null, 1);
        SQLiteDatabase db = admin.getReadableDatabase();

        // Validar duplicados de materia para este estudiante (Mismo código)
        String queryCheck = "SELECT id FROM materias WHERE codigo_materia = ? AND id_estudiante = ?";
        String[] argsCheck = new String[]{code, String.valueOf(studentId)};

        if (subjectId != -1) {
            queryCheck += " AND id != ?";
            argsCheck = new String[]{code, String.valueOf(studentId), String.valueOf(subjectId)};
        }

        Cursor cursor = db.rawQuery(queryCheck, argsCheck);
        if (cursor.getCount() > 0) {
            Toast.makeText(this, "Este estudiante ya tiene inscrita esta materia (código duplicado)", Toast.LENGTH_LONG).show();
            cursor.close();
            db.close();
            return;
        }
        cursor.close();

        // Guardar Docente (Tabla oculta/referencial)
        db = admin.getWritableDatabase();
        // Insertamos el docente si no existe solo por mantener el registro, aunque en materias usamos el string nombre
        db.execSQL("INSERT INTO docentes (nombre_completo) SELECT ? WHERE NOT EXISTS(SELECT 1 FROM docentes WHERE nombre_completo = ?)", new Object[]{teacher, teacher});

        // Guardar Materia
        ContentValues values = new ContentValues();
        values.put("codigo_materia", code);
        values.put("nombre", name);
        values.put("hora_inicio", start);
        values.put("hora_fin", end);
        values.put("aula", classroom);
        values.put("nombre_docente", teacher);
        values.put("id_estudiante", studentId);

        long result;
        if (subjectId == -1) {
            result = db.insert("materias", null, values);
            if (result != -1) Toast.makeText(this, "Materia agregada", Toast.LENGTH_SHORT).show();
        } else {
            result = db.update("materias", values, "id = ?", new String[]{String.valueOf(subjectId)});
            if (result > 0) Toast.makeText(this, "Materia actualizada", Toast.LENGTH_SHORT).show();
        }
        db.close();

        if (result != -1 && result != 0) {
            setResult(Activity.RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Materia")
                .setMessage("¿Estás seguro de eliminar esta asignatura?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this, "bd_cualma", null, 1);
                    SQLiteDatabase db = admin.getWritableDatabase();
                    db.delete("materias", "id = ?", new String[]{String.valueOf(subjectId)});
                    db.close();
                    Toast.makeText(this, "Asignatura eliminada", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK);
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}