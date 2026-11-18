using UnityEngine;

namespace DreamMMORPG.Player
{
    [RequireComponent(typeof(CharacterController))]
    public sealed class TopDownPlayerController : MonoBehaviour
    {
        [SerializeField] private float moveSpeed = 5f;
        [SerializeField] private float rotationSpeed = 720f;
        [SerializeField] private float gravity = -20f;
        [SerializeField] private float joystickRadius = 110f;

        private CharacterController controller;
        private Vector3 verticalVelocity;
        private Vector2 touchMove;
        private int movementFingerId = -1;

        private void Awake()
        {
            controller = GetComponent<CharacterController>();
        }

        private void Update()
        {
            Vector2 input = ReadMovement();
            Vector3 direction = new Vector3(input.x, 0f, input.y);

            if (direction.sqrMagnitude > 1f)
                direction.Normalize();

            if (direction.sqrMagnitude > 0.001f)
            {
                Quaternion targetRotation = Quaternion.LookRotation(direction, Vector3.up);
                transform.rotation = Quaternion.RotateTowards(
                    transform.rotation, targetRotation, rotationSpeed * Time.deltaTime);
            }

            if (controller.isGrounded && verticalVelocity.y < 0f)
                verticalVelocity.y = -2f;

            verticalVelocity.y += gravity * Time.deltaTime;
            Vector3 velocity = direction * moveSpeed + verticalVelocity;
            controller.Move(velocity * Time.deltaTime);
        }

        private Vector2 ReadMovement()
        {
            Vector2 keyboard = new Vector2(Input.GetAxisRaw("Horizontal"), Input.GetAxisRaw("Vertical"));
            if (keyboard.sqrMagnitude > 0.01f)
                return Vector2.ClampMagnitude(keyboard, 1f);

            touchMove = Vector2.zero;

            for (int i = 0; i < Input.touchCount; i++)
            {
                Touch touch = Input.GetTouch(i);

                if (touch.phase == TouchPhase.Began && movementFingerId == -1 &&
                    touch.position.x < Screen.width * 0.5f)
                    movementFingerId = touch.fingerId;

                if (touch.fingerId != movementFingerId)
                    continue;

                if (touch.phase == TouchPhase.Ended || touch.phase == TouchPhase.Canceled)
                {
                    movementFingerId = -1;
                    touchMove = Vector2.zero;
                    break;
                }

                Vector2 delta = touch.position - touch.rawPosition;
                touchMove = Vector2.ClampMagnitude(delta / Mathf.Max(joystickRadius, 1f), 1f);
            }

            return touchMove;
        }
    }
}
