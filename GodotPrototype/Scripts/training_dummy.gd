extends StaticBody3D
class_name TrainingDummy

@export var max_health: int = 100
var health: int

func _ready() -> void:
	health = max_health

func take_damage(amount: int) -> void:
	health = max(health - amount, 0)
	if health == 0:
		health = max_health
