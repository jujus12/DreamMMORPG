using UnityEngine;

namespace DreamMMORPG.Combat
{
    public sealed class HybridCombatController : MonoBehaviour
    {
        [SerializeField] private Camera gameplayCamera;
        [SerializeField] private LayerMask targetLayers = ~0;
        [SerializeField] private float attackRange = 3f;
        [SerializeField] private int basicAttackDamage = 20;
        [SerializeField] private float attackCooldown = 0.8f;
        [SerializeField] private float abilityCooldown = 4f;
        [SerializeField] private int abilityDamage = 45;

        public Targetable CurrentTarget { get; private set; }
        private float nextAttackTime;
        private float nextAbilityTime;

        private void Update()
        {
            if (gameplayCamera == null)
                gameplayCamera = Camera.main;

            HandleTargetInput();
            HandleAttackInput();
        }

        private void HandleTargetInput()
        {
            bool pressed = Input.GetMouseButtonDown(0);
            Vector2 screenPosition = Input.mousePosition;

            if (Input.touchCount > 0)
            {
                Touch touch = Input.GetTouch(0);
                if (touch.phase == TouchPhase.Began && touch.position.x >= Screen.width * 0.5f)
                {
                    pressed = true;
                    screenPosition = touch.position;
                }
            }

            if (!pressed || gameplayCamera == null)
                return;

            Ray ray = gameplayCamera.ScreenPointToRay(screenPosition);
            if (Physics.Raycast(ray, out RaycastHit hit, 250f, targetLayers))
            {
                Targetable found = hit.collider.GetComponentInParent<Targetable>();
                if (found != null && found.IsAlive)
                    CurrentTarget = found;
            }
        }

        private void HandleAttackInput()
        {
            if (Input.GetKeyDown(KeyCode.Space))
                BasicAttack();

            if (Input.GetKeyDown(KeyCode.Q))
                UseAbility();

            if (Input.touchCount > 0)
            {
                Touch touch = Input.GetTouch(0);
                if (touch.phase == TouchPhase.Began && touch.position.x >= Screen.width * 0.82f &&
                    touch.position.y < Screen.height * 0.35f)
                    BasicAttack();
            }
        }

        public void BasicAttack()
        {
            if (!CanAttack() || Time.time < nextAttackTime)
                return;

            nextAttackTime = Time.time + attackCooldown;
            CurrentTarget.TakeDamage(basicAttackDamage);
        }

        public void UseAbility()
        {
            if (!CanAttack() || Time.time < nextAbilityTime)
                return;

            nextAbilityTime = Time.time + abilityCooldown;
            CurrentTarget.TakeDamage(abilityDamage);
        }

        private bool CanAttack()
        {
            if (CurrentTarget == null || !CurrentTarget.IsAlive)
                return false;

            return Vector3.Distance(transform.position, CurrentTarget.transform.position) <= attackRange;
        }
    }
}
