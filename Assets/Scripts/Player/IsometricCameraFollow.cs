using UnityEngine;

namespace DreamMMORPG.Player
{
    public sealed class IsometricCameraFollow : MonoBehaviour
    {
        [SerializeField] private Transform target;
        [SerializeField] private Camera controlledCamera;
        [SerializeField] private Vector3 offset = new Vector3(0f, 12f, -9f);
        [SerializeField] private float followSharpness = 8f;
        [SerializeField] private float lookHeight = 1f;
        [SerializeField] private float orthographicSize = 8f;

        private void LateUpdate()
        {
            if (target == null)
                return;

            Transform cameraTransform = controlledCamera != null ? controlledCamera.transform : transform;
            Vector3 desiredPosition = target.position + offset;
            cameraTransform.position = Vector3.Lerp(
                cameraTransform.position, desiredPosition, 1f - Mathf.Exp(-followSharpness * Time.deltaTime));
            cameraTransform.LookAt(target.position + Vector3.up * lookHeight);

            if (controlledCamera != null)
                controlledCamera.orthographicSize = orthographicSize;
        }

        public void SetTarget(Transform newTarget) => target = newTarget;
    }
}
