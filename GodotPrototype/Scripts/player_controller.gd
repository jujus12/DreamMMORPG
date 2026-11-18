extends CharacterBody3D
class_name PlayerController

@export var move_speed: float = 5.0
@export var gravity: float = 18.0
@export var attack_range: float = 3.2
@export var attack_damage: int = 25

var selected_target: Node3D
var destination: Vector3
var has_destination := false
var skill_cooldowns: Dictionary = {}

func _physics_process(delta: float) -> void:
	var input_dir := Input.get_vector("move_left", "move_right", "move_forward", "move_back")
	if input_dir.length() > 0.1:
		has_destination = false
		var direction := Vector3(input_dir.x, 0.0, input_dir.y).normalized()
		velocity.x = direction.x * move_speed
		velocity.z = direction.z * move_speed
	elif has_destination:
		var to_goal := destination - global_position
		to_goal.y = 0.0
		if to_goal.length() < 0.25:
			has_destination = false
			velocity.x = 0.0
			velocity.z = 0.0
		else:
			var direction := to_goal.normalized()
			velocity.x = direction.x * move_speed
			velocity.z = direction.z * move_speed
	else:
		velocity.x = move_toward(velocity.x, 0.0, move_speed * 5.0 * delta)
		velocity.z = move_toward(velocity.z, 0.0, move_speed * 5.0 * delta)
	if not is_on_floor():
		velocity.y -= gravity * delta
	else:
		velocity.y = 0.0
	move_and_slide()

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventScreenTouch and event.pressed:
		_handle_world_tap(event.position)
	elif event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT:
		_handle_world_tap(event.position)
	elif event.is_action_pressed("attack"):
		basic_attack()

func _handle_world_tap(screen_position: Vector2) -> void:
	var camera := get_viewport().get_camera_3d()
	if camera == null:
		return
	var ray_from := camera.project_ray_origin(screen_position)
	var ray_to := ray_from + camera.project_ray_normal(screen_position) * 200.0
	var query := PhysicsRayQueryParameters3D.create(ray_from, ray_to)
	var hit := get_world_3d().direct_space_state.intersect_ray(query)
	if hit.is_empty():
		return
	var collider: Object = hit.get("collider")
	if collider is Node3D and collider.has_method("take_damage"):
		select_target(collider as Node3D)
	else:
		set_move_destination(hit.get("position", global_position))

func set_move_destination(point: Vector3) -> void:
	destination = point
	destination.y = global_position.y
	has_destination = true

func select_target(target: Node3D) -> void:
	selected_target = target

func basic_attack() -> void:
	if _target_in_range():
		selected_target.take_damage(attack_damage)

func use_skill(skill_name: String) -> void:
	if float(skill_cooldowns.get(skill_name, 0.0)) > 0.0:
		return
	match skill_name:
		"Cleave":
			_damage_nearby(35, 3.5)
			skill_cooldowns[skill_name] = 2.0
		"Overhead Strike":
			if _target_in_range():
				selected_target.take_damage(60)
			skill_cooldowns[skill_name] = 3.0
		"Whirlwind":
			_damage_nearby(25, 4.5)
			skill_cooldowns[skill_name] = 6.0
		"Charge":
			_charge_toward_target()
			skill_cooldowns[skill_name] = 5.0

func _process(delta: float) -> void:
	for skill in skill_cooldowns.keys():
		skill_cooldowns[skill] = maxf(float(skill_cooldowns[skill]) - delta, 0.0)

func _target_in_range() -> bool:
	return is_instance_valid(selected_target) and global_position.distance_to(selected_target.global_position) <= attack_range

func _damage_nearby(amount: int, radius: float) -> void:
	for item in get_tree().get_nodes_in_group("damageable"):
		if item is Node3D and global_position.distance_to(item.global_position) <= radius:
			item.take_damage(amount)

func _charge_toward_target() -> void:
	if not is_instance_valid(selected_target):
		return
	var direction := selected_target.global_position - global_position
	direction.y = 0.0
	if direction.length() > 0.1:
		global_position += direction.normalized() * minf(maxf(direction.length() - 1.5, 0.0), 4.0)
		selected_target.take_damage(45)
