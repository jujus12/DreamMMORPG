extends CanvasLayer

@export var player_path: NodePath
var player: PlayerController
var status_label: Label

func _ready() -> void:
	player = get_node_or_null(player_path) as PlayerController
	var panel := HBoxContainer.new()
	panel.anchor_left = 0.0
	panel.anchor_top = 1.0
	panel.anchor_right = 1.0
	panel.anchor_bottom = 1.0
	panel.offset_left = 12
	panel.offset_top = -76
	panel.offset_right = -12
	panel.offset_bottom = -12
	panel.add_theme_constant_override("separation", 8)
	add_child(panel)

	_add_button(panel, "Vur", "attack")
	_add_button(panel, "Cleave", "Cleave")
	_add_button(panel, "Ağır Darbe", "Overhead Strike")
	_add_button(panel, "Kasırga", "Whirlwind")
	_add_button(panel, "Hücum", "Charge")

	status_label = Label.new()
	status_label.text = "Hedef: ekrana dokun | Yere dokun: hareket"
	status_label.position = Vector2(16, 16)
	status_label.add_theme_font_size_override("font_size", 20)
	add_child(status_label)

func _add_button(parent: HBoxContainer, caption: String, action: String) -> void:
	var button := Button.new()
	button.text = caption
	button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	button.custom_minimum_size = Vector2(90, 54)
	button.add_theme_font_size_override("font_size", 16)
	button.pressed.connect(_on_action.bind(action))
	parent.add_child(button)

func _on_action(action: String) -> void:
	if player == null:
		return
	if action == "attack":
		player.basic_attack()
	else:
		player.use_skill(action)
