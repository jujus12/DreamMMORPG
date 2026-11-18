extends StaticBody3D
class_name TrainingDummy

@export var max_health: int = 100
var health: int

func _ready() -> void:
	health = max_health
	add_to_group("damageable")

func take_damage(amount: int) -> void:
	health = maxi(health - amount, 0)
	print("Training dummy HP: ", health, "/", max_health)
	if health == 0:
		health = max_health
		print("Training dummy respawned.")
