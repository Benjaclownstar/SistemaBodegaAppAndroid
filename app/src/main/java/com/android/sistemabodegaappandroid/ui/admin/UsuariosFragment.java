package com.android.sistemabodegaappandroid.ui.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.sistemabodegaappandroid.R;
import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.model.Rol;
import com.android.sistemabodegaappandroid.model.Usuario;
import com.android.sistemabodegaappandroid.ui.adapter.UsuarioAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

public class UsuariosFragment extends Fragment {

    private RecyclerView rvUsuarios;
    private TextView tvEmptyUsuarios;
    private FloatingActionButton fabAddUser;
    private DatabaseHelper dbHelper;
    private UsuarioAdapter adapter;
    private List<Usuario> usuariosList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_usuarios, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());

        rvUsuarios = view.findViewById(R.id.rvUsuarios);
        tvEmptyUsuarios = view.findViewById(R.id.tvEmptyUsuarios);
        fabAddUser = view.findViewById(R.id.fabAddUser);

        rvUsuarios.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new UsuarioAdapter(usuariosList, (usuario, isChecked) -> {
            boolean updated = dbHelper.updateUsuarioEstado(usuario.getIdUsuario(), isChecked ? 1 : 0);
            if (updated) {
                usuario.setActivo(isChecked ? 1 : 0);
                Toast.makeText(requireContext(), "Estado del usuario actualizado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Error al actualizar estado", Toast.LENGTH_SHORT).show();
            }
        });
        rvUsuarios.setAdapter(adapter);

        fabAddUser.setOnClickListener(v -> showAddUserDialog());

        loadUsuarios();
    }

    public void loadUsuarios() {
        usuariosList = dbHelper.getAllUsuarios();
        if (usuariosList.isEmpty()) {
            tvEmptyUsuarios.setVisibility(View.VISIBLE);
            rvUsuarios.setVisibility(View.GONE);
        } else {
            tvEmptyUsuarios.setVisibility(View.GONE);
            rvUsuarios.setVisibility(View.VISIBLE);
            adapter.updateData(usuariosList);
        }
    }

    private void showAddUserDialog() {
        List<Rol> roles = dbHelper.getAllRoles();
        if (roles.isEmpty()) {
            Toast.makeText(requireContext(), "Debe registrar al menos un rol antes de crear usuarios", Toast.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_usuario, null);
        EditText etUsername = dialogView.findViewById(R.id.etUsername);
        EditText etNombreCompleto = dialogView.findViewById(R.id.etNombreCompleto);
        EditText etPassword = dialogView.findViewById(R.id.etPassword);
        Spinner spinnerRoles = dialogView.findViewById(R.id.spinnerRoles);
        MaterialSwitch switchActivo = dialogView.findViewById(R.id.switchActivoDialog);

        ArrayAdapter<Rol> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, roles);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoles.setAdapter(spinnerAdapter);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_user)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
                    String nombreCompleto = etNombreCompleto.getText() != null ? etNombreCompleto.getText().toString().trim() : "";
                    String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
                    Rol selectedRol = (Rol) spinnerRoles.getSelectedItem();
                    boolean activo = switchActivo.isChecked();

                    if (TextUtils.isEmpty(username) || TextUtils.isEmpty(nombreCompleto) || TextUtils.isEmpty(password)) {
                        Toast.makeText(requireContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (selectedRol == null) {
                        Toast.makeText(requireContext(), "Seleccione un rol válido", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (dbHelper.isUsernameExists(username)) {
                        Toast.makeText(requireContext(), "El nombre de usuario ya existe (debe ser único)", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Usuario nuevoUsuario = new Usuario(username, nombreCompleto, "", selectedRol.getIdRol(), activo ? 1 : 0);
                    long result = dbHelper.insertUsuario(nuevoUsuario, password);
                    if (result != -1) {
                        Toast.makeText(requireContext(), "Usuario registrado con éxito", Toast.LENGTH_SHORT).show();
                        loadUsuarios();
                    } else {
                        Toast.makeText(requireContext(), "Error al registrar el usuario", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
