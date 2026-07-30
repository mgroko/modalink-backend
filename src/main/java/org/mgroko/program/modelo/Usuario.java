package org.mgroko.program.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_usuario")
	private Long idUsuario;

	@Column(name = "nombre", nullable = false, length = 50)
	private String nombre;

	@Column(name = "apellido", nullable = false, length = 50)
	private String apellido;

	@Column(name = "dni", nullable = false, length = 15)
	private String dni;

	@Column(name = "fecha_nacimiento", nullable = false)
	private LocalDate fechaNacimiento;

	@Column(name = "correo", nullable = false, unique = true, length = 255)
	private String correo;

	@Column(name = "password_hash", length = 255)
	private String passwordHash;

	@Column(name = "proveedor_auth", nullable = false, length = 20)
	private String proveedorAuth;

	@Column(name = "id_externo", length = 100)
	private String idExterno;

	@Column(name = "estado", nullable = false, length = 20)
	private String estado;

	@Column(name = "fecha_solicitud_baja")
	private LocalDateTime fechaSolicitudBaja;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_ubicacion")
	private Ubicacion ubicacion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_rol_global", nullable = false)
	private RolGlobal rolGlobal;

	public Usuario() {
	}

	public Long getIdUsuario() {
		return idUsuario;
	}

	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getApellido() {
		return apellido;
	}

	public void setApellido(String apellido) {
		this.apellido = apellido;
	}

	public String getDni() {
		return dni;
	}

	public void setDni(String dni) {
		this.dni = dni;
	}

	public LocalDate getFechaNacimiento() {
		return fechaNacimiento;
	}

	public void setFechaNacimiento(LocalDate fechaNacimiento) {
		this.fechaNacimiento = fechaNacimiento;
	}

	public String getCorreo() {
		return correo;
	}

	public void setCorreo(String correo) {
		this.correo = correo;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getProveedorAuth() {
		return proveedorAuth;
	}

	public void setProveedorAuth(String proveedorAuth) {
		this.proveedorAuth = proveedorAuth;
	}

	public String getIdExterno() {
		return idExterno;
	}

	public void setIdExterno(String idExterno) {
		this.idExterno = idExterno;
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	public LocalDateTime getFechaSolicitudBaja() {
		return fechaSolicitudBaja;
	}

	public void setFechaSolicitudBaja(LocalDateTime fechaSolicitudBaja) {
		this.fechaSolicitudBaja = fechaSolicitudBaja;
	}

	public Ubicacion getUbicacion() {
		return ubicacion;
	}

	public void setUbicacion(Ubicacion ubicacion) {
		this.ubicacion = ubicacion;
	}

	public RolGlobal getRolGlobal() {
		return rolGlobal;
	}

	public void setRolGlobal(RolGlobal rolGlobal) {
		this.rolGlobal = rolGlobal;
	}
}
