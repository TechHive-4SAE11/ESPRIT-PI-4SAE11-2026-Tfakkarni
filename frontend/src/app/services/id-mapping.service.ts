import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class IdMappingService {

    private readonly STORAGE_KEY = 'tfakkarni_name_mappings';

    // Charge les mappings depuis localStorage
    private loadMappings(): { [normalizedName: string]: string } {
        const stored = localStorage.getItem(this.STORAGE_KEY);
        return stored ? JSON.parse(stored) : {};
    }

    // Sauvegarde les mappings
    private saveMappings(mappings: { [key: string]: string }): void {
        localStorage.setItem(this.STORAGE_KEY, JSON.stringify(mappings));
    }

    // Nettoie un nom pour l'utiliser comme clé
    normalizeName(name: string): string {
        return name.toLowerCase().trim().replace(/\s+/g, ' ');
    }

    // Génère un ID à partir d'un nom (ou récupère l'existant)
    getIdForName(name: string, type: 'patient' | 'doctor'): string {
        if (!name) return '';

        const mappings = this.loadMappings();
        const normalizedName = this.normalizeName(name);

        // Si on a déjà un mapping, on le réutilise
        if (mappings[normalizedName]) {
            return mappings[normalizedName];
        }

        // Sinon, génère un nouvel ID
        const baseId = name.toLowerCase()
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .replace(/[^a-z0-9]/g, '-')
            .replace(/-+/g, '-')
            .replace(/^-|-$/g, '');

        const newId = `${type}-${baseId}`;

        // Sauvegarde le mapping
        mappings[normalizedName] = newId;
        this.saveMappings(mappings);

        return newId;
    }

    // Récupère le nom à partir d'un ID (pour le pré-remplissage en modification)
    getNameFromId(id: string): string | null {
        if (!id) return null;
        const mappings = this.loadMappings();
        for (const [name, storedId] of Object.entries(mappings)) {
            if (storedId === id) {
                return name;
            }
        }
        return null;
    }

    // Récupère tous les noms pour l'autocomplétion
    getAllNames(type?: 'patient' | 'doctor'): { name: string; id: string }[] {
        const mappings = this.loadMappings();
        return Object.entries(mappings)
            .filter(([_, id]) => !type || id.startsWith(type))
            .map(([name, id]) => ({ name, id }));
    }
}
