* Hogyan tudok spawnolni szörnyet?
            tempent = GameUtil.G_Spawn();
            Math3D.VectorCopy(self.s.origin, tempent.s.origin);
            Math3D.VectorCopy(self.s.angles, tempent.s.angles);
            tempent.s.origin[1] -= 84;
            makron_torso.think(tempent);