const fs = require('fs');
const path = require('path');

const dir = 'C:/Users/Lenovo/OneDrive - ESPRIT/Documents/GitHub/ESPRIT-PI-4SAE11-2026-Tfakkarni/frontend/src/app/pages/patient-dashboard';

const replacements = {
    'Équipements': 'Equipment',
    'équipements': 'equipment',
    'Équipement': 'Equipment',
    'équipement': 'equipment',
    "Erreur lors de l'emprunt.": 'Error during borrowing.',
    'emprunté avec succès': 'borrowed successfully',
    "Empruntez un équipement depuis l'onglet Équipements.": 'Borrow equipment from the Equipment tab.',
    "Vous n'avez pas d'équipement emprunté en ce moment.": 'You do not have any borrowed equipment at the moment.',
    'Aucun équipement trouvé': 'No equipment found',
    'Ajoutez un équipement ou modifiez les filtres.': 'Add equipment or modify filters.',
    'Rechercher un équipement...': 'Search for equipment...',
    'Gérez les équipements médicaux et les prêts': 'Manage medical equipment and loans',
    'Total Équipements': 'Total Equipment',
    'Enregistrez un équipement médical donné.': 'Record a donated medical equipment.',
    "Nom de l'équipement...": 'Equipment name...',
    "Nom de l'équipement": 'Equipment name',
    "Détails de l'Équipement": 'Equipment Details',
    'Modifier Équipement': 'Edit Equipment',
    'Ajouter Équipement': 'Add Equipment',
    'Erreur lors du chargement des équipements': 'Error loading equipment',
    'Erreur lors du chargement des prêts': 'Error loading loans',
    'Erreur lors du chargement': 'Error loading',
    'Erreur lors de la vérification': 'Error during verification',
    'Erreur lors de la sauvegarde': 'Error saving',
    'Erreur de sauvegarde': 'Save error',
    'Erreur de suppression': 'Deletion error',
    'Erreur de chargement des questions': 'Error loading questions',
    'Erreur lors de la création. Vérifiez la console.': 'Error during creation. Check console.',
    'Veuillez remplir tous les champs obligatoires.': 'Please fill all required fields.',
    'Erreur lors du retour.': 'Error returning.',
    'Erreur lors de la prolongation.': 'Error extending.',
    "Erreur lors de l'annulation.": 'Error cancelling.',
    'Prêt annulé.': 'Loan cancelled.',
    'retourné !': 'returned!',
    'prolongé de': 'extended by',
    'jours !': 'days!',
    'Équipement retourné avec succès!': 'Equipment returned successfully!',
    'Équipement mis à jour avec succès': 'Equipment updated successfully',
    'Équipement créé avec succès': 'Equipment created successfully',
    'Équipement supprimé avec succès': 'Equipment deleted successfully',
    'Don enregistré avec succès!': 'Donation recorded successfully!',
    'Question + Réponses créées avec succès !': 'Question and Answers created successfully!',
    'Toutes les questions supprimées': 'All questions deleted',
    'Question supprimée': 'Question deleted',
    'Question modifiée': 'Question modified',
    'Choix de réponse ajouté': 'Answer choice added',
    'Texte mis à jour': 'Text updated',
    'Explication mise à jour': 'Explanation updated',
    'Bonne réponse définie': 'Correct answer set',
    'Réponse supprimée': 'Answer deleted',
    'Quiz modifié': 'Quiz modified',
    'Quiz créé': 'Quiz created',
    'Quiz supprimé': 'Quiz deleted',
    'Quizz avec score': 'Quizzes with score',
    'trouvés': 'found',
    'Veuillez sélectionner une date de début et de fin': 'Please select a start and end date',
    'Quizz filtrés par date': 'Quizzes filtered by date',
    'Backend vérifie:': 'Backend verified:',
    'Nom et catégorie sont requis': 'Name and category are required',
    'disponible(s)': 'available',
    'avec des prêts en retard': 'with overdue loans',
    'prêt(s) dûs dans les 3 prochains jours': 'loan(s) due in the next 3 days',
    'Prêts en retard mis à jour': 'Overdue loans updated',
    "Impossible de supprimer: cet équipement est lié à des prêts existants. Annulez d'abord ses prêts.": 'Cannot delete: this equipment is linked to existing loans.Cancel its loans first.',
    'Confirmer le retour de cet équipement?': 'Confirm return of this equipment?',
    'retourné': 'returned',
    'Statut mis à jour': 'Status updated',
    'Erreur lors de la mise à jour du statut': 'Error updating status',
    "Erreur lors de l'enregistrement du don": 'Error recording donation',
    'Erreur lors de la suppression': 'Error during deletion'
};

function walk(directory) {
    let results = [];
    const list = fs.readdirSync(directory);
    list.forEach(file => {
        file = path.join(directory, file);
        const stat = fs.statSync(file);
        if (stat && stat.isDirectory()) {
            results = results.concat(walk(file));
        } else if (file.endsWith('.ts') || file.endsWith('.html')) {
            results.push(file);
        }
    });
    return results;
}

const files = walk(dir);
files.forEach(file => {
    let content = fs.readFileSync(file, 'utf8');
    let changed = false;

    for (const [fr, en] of Object.entries(replacements)) {
        if (content.includes(fr)) {
            content = content.split(fr).join(en);
            changed = true;
        }
    }

    if (changed) {
        fs.writeFileSync(file, content, 'utf8');
        console.log('Updated: ' + file);
    }
});
