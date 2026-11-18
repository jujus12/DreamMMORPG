extends CharacterBody3D
class_name PlayerController

@export var move_speed: float = 5.0
@export var gravity: float = 18.0
@export var attack_range: float = 3.0
@export var attack_damage: int = 25

var selected_target: Node3D
var destination: Vector3
var has_destination := false

func _physics_process(delta: float) -> void:
	var input_dir := Input.get_vector("move_left", "move_right", "move_forward", "move_back")
	if input_dir.length() > 0.1:
		has_destination = false
		var direction := Vector3(input_dir.x, 0.0, input_dir.y)
		velocity.x = direction.x * move_speed
		velocity.z = direction.z * move_speed
	elif has_destination:
		var to_goal := destination - global_position
		to_goal.y = 0.0
		if to_goal.length() < 0.2:
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

func set_move_destination(point: Vector3) -> void:
	destination = point
	has_destination = true

func select_target(target: Node3D) -> void:
	selected_target = target

func basic_attack() -> void:
	if is_instance_valid(selected_target) and global_position.distance_to(selected_target.global_position) <= attack_range:
		if selected_target.has_method("take_damage"):
			selected_target.take_damage(attack_damage)
