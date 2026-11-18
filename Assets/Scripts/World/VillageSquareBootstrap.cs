using UnityEngine;

namespace DreamMMORPG.World
{
    /// <summary>
    /// Builds a lightweight placeholder village hub from Unity primitives.
    /// Add this component to an empty GameObject in a new scene and enter Play mode.
    /// Replace primitives with authored models as the project art is produced.
    /// </summary>
    public sealed class VillageSquareBootstrap : MonoBehaviour
    {
        [SerializeField] private bool buildOnStart = true;
        [SerializeField] private int dummyCount = 4;
        [SerializeField] private int npcCount = 3;

        private Material groundMat, stoneMat, woodMat, leafMat, clothMat, roofMat, skinMat, dummyMat;

        private void Start()
        {
            if (buildOnStart) Build();
        }

        [ContextMenu("Build Village Square")]
        public void Build()
        {
            if (transform.childCount > 0) return;
            CreateMaterials();
            BuildGround();
            BuildCentralTree();
            BuildBuildings();
            BuildNpcs();
            BuildTrainingDummies();
            BuildLanterns();
        }

        private void CreateMaterials()
        {
            groundMat = MakeMat(new Color(0.22f, 0.29f, 0.19f));
            stoneMat = MakeMat(new Color(0.38f, 0.39f, 0.37f));
            woodMat = MakeMat(new Color(0.22f, 0.12f, 0.07f));
            leafMat = MakeMat(new Color(0.12f, 0.28f, 0.13f));
            clothMat = MakeMat(new Color(0.36f, 0.25f, 0.16f));
            roofMat = MakeMat(new Color(0.19f, 0.12f, 0.10f));
            skinMat = MakeMat(new Color(0.68f, 0.48f, 0.34f));
            dummyMat = MakeMat(new Color(0.48f, 0.29f, 0.14f));
        }

        private Material MakeMat(Color color)
        {
            Shader shader = Shader.Find("Universal Render Pipeline/Lit");
            if (shader == null) shader = Shader.Find("Standard");
            Material material = new Material(shader);
            material.color = color;
            return material;
        }

        private GameObject Primitive(PrimitiveType type, string objectName, Vector3 position,
            Vector3 scale, Material material, Transform parent = null)
        {
            GameObject go = GameObject.CreatePrimitive(type);
            go.name = objectName;
            go.transform.SetParent(parent != null ? parent : transform, false);
            go.transform.localPosition = position;
            go.transform.localScale = scale;
            if (material != null) go.GetComponent<Renderer>().sharedMaterial = material;
            return go;
        }

        private void BuildGround()
        {
            Primitive(PrimitiveType.Plane, "Grass Ground", Vector3.zero,
                new Vector3(5f, 1f, 5f), groundMat);
            Primitive(PrimitiveType.Cube, "Stone Square", new Vector3(0f, 0.025f, 0f),
                new Vector3(15f, 0.12f, 15f), stoneMat);

            for (int x = -7; x <= 7; x += 2)
                for (int z = -7; z <= 7; z += 2)
                    Primitive(PrimitiveType.Cube, "Paving Stone",
                        new Vector3(x, 0.10f, z), new Vector3(1.85f, 0.08f, 1.85f),
                        MakeMat(new Color(0.42f + Random.Range(-0.035f, 0.035f),
                                          0.42f + Random.Range(-0.035f, 0.035f),
                                          0.40f)), transform);
        }

        private void BuildCentralTree()
        {
            Primitive(PrimitiveType.Cylinder, "Ancient Tree Trunk", new Vector3(0f, 2.2f, 0f),
                new Vector3(0.85f, 2.2f, 0.85f), woodMat);
            Primitive(PrimitiveType.Sphere, "Ancient Tree Canopy", new Vector3(0f, 5.2f, 0f),
                new Vector3(5.4f, 4.1f, 5.4f), leafMat);
            Primitive(PrimitiveType.Sphere, "Canopy Upper Crown", new Vector3(0.4f, 7.0f, 0.2f),
                new Vector3(3.7f, 3.0f, 3.7f), leafMat);
            for (int i = 0; i < 4; i++)
            {
                float angle = i * 90f;
                GameObject root = Primitive(PrimitiveType.Cylinder, "Tree Root",
                    new Vector3(Mathf.Sin(angle * Mathf.Deg2Rad) * 1.1f, 0.25f,
                                Mathf.Cos(angle * Mathf.Deg2Rad) * 1.1f),
                    new Vector3(0.28f, 0.28f, 1.5f), woodMat);
                root.transform.rotation = Quaternion.Euler(0f, angle, 65f);
            }
        }

        private void BuildBuildings()
        {
            BuildHut(new Vector3(-12f, 1.5f, 8f), "Inn");
            BuildHut(new Vector3(12f, 1.5f, 8f), "Trader House");
            BuildHut(new Vector3(-12f, 1.5f, -9f), "Guard House");
        }

        private void BuildHut(Vector3 position, string hutName)
        {
            Primitive(PrimitiveType.Cube, hutName + " Walls", position,
                new Vector3(6f, 3f, 5f), clothMat);
            Primitive(PrimitiveType.Cube, hutName + " Roof", position + Vector3.up * 2.1f,
                new Vector3(6.8f, 0.55f, 5.8f), roofMat);
            Primitive(PrimitiveType.Cube, hutName + " Door", position + new Vector3(0f, -0.45f, -2.53f),
                new Vector3(1.2f, 2.1f, 0.12f), woodMat);
        }

        private void BuildNpcs()
        {
            for (int i = 0; i < npcCount; i++)
            {
                float angle = i * Mathf.PI * 2f / Mathf.Max(1, npcCount);
                Vector3 p = new Vector3(Mathf.Cos(angle) * 10f, 0f, Mathf.Sin(angle) * 10f);
                GameObject npc = new GameObject("NPC Placeholder " + (i + 1));
                npc.transform.SetParent(transform, false);
                npc.transform.localPosition = p;
                Primitive(PrimitiveType.Capsule, "NPC Body", new Vector3(0f, 1f, 0f),
                    new Vector3(0.65f, 1f, 0.65f), clothMat, npc.transform);
                Primitive(PrimitiveType.Sphere, "NPC Head", new Vector3(0f, 2.15f, 0f),
                    new Vector3(0.55f, 0.55f, 0.55f), skinMat, npc.transform);
            }
        }

        private void BuildTrainingDummies()
        {
            for (int i = 0; i < dummyCount; i++)
            {
                float x = -5f + i * 3.3f;
                Vector3 p = new Vector3(x, 0f, -12f);
                GameObject dummy = new GameObject("Training Dummy " + (i + 1));
                dummy.transform.SetParent(transform, false);
                dummy.transform.localPosition = p;
                Primitive(PrimitiveType.Cylinder, "Dummy Post", new Vector3(0f, 1.2f, 0f),
                    new Vector3(0.22f, 1.2f, 0.22f), woodMat, dummy.transform);
                Primitive(PrimitiveType.Capsule, "Dummy Body", new Vector3(0f, 2.0f, 0f),
                    new Vector3(0.72f, 0.72f, 0.45f), dummyMat, dummy.transform);
                Primitive(PrimitiveType.Cube, "Dummy Crossbar", new Vector3(0f, 2.15f, 0f),
                    new Vector3(1.5f, 0.16f, 0.18f), woodMat, dummy.transform);
            }
        }

        private void BuildLanterns()
        {
            Vector3[] positions = {
                new Vector3(-8f, 2f, 0f), new Vector3(8f, 2f, 0f),
                new Vector3(0f, 2f, 8f), new Vector3(0f, 2f, -8f)
            };
            foreach (Vector3 p in positions)
            {
                Primitive(PrimitiveType.Cylinder, "Lantern Post", p,
                    new Vector3(0.12f, 2f, 0.12f), woodMat);
                GameObject lightObject = new GameObject("Warm Lantern Light");
                lightObject.transform.SetParent(transform, false);
                lightObject.transform.localPosition = p + Vector3.up * 0.5f;
                Light point = lightObject.AddComponent<Light>();
                point.type = LightType.Point;
                point.color = new Color(1f, 0.68f, 0.36f);
                point.intensity = 1.2f;
                point.range = 7f;
            }
        }
    }
}
