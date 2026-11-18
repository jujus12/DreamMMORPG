using UnityEngine;

namespace DreamMMORPG.Combat
{
    public sealed class Targetable : MonoBehaviour
    {
        [SerializeField] private string displayName = "Training Monster";
        [SerializeField] private int maxHealth = 100;
        [SerializeField] private float respawnSeconds = 5f;

        public string DisplayName => displayName;
        public int CurrentHealth { get; private set; }
        public bool IsAlive => CurrentHealth > 0;

        private Renderer[] renderers;
        private Collider[] colliders;
        private Vector3 spawnPosition;

        private void Awake()
        {
            CurrentHealth = Mathf.Max(1, maxHealth);
            spawnPosition = transform.position;
            renderers = GetComponentsInChildren<Renderer>();
            colliders = GetComponentsInChildren<Collider>();
        }

        public void TakeDamage(int amount)
        {
            if (!IsAlive || amount <= 0)
                return;

            CurrentHealth = Mathf.Max(0, CurrentHealth - amount);
            if (CurrentHealth == 0)
                Die();
        }

        private void Die()
        {
            foreach (Renderer item in renderers) item.enabled = false;
            foreach (Collider item in colliders) item.enabled = false;
            Invoke(nameof(Respawn), respawnSeconds);
        }

        private void Respawn()
        {
            transform.position = spawnPosition;
            CurrentHealth = Mathf.Max(1, maxHealth);
            foreach (Renderer item in renderers) item.enabled = true;
            foreach (Collider item in colliders) item.enabled = true;
        }
    }
}
