// Fonction pour afficher les détails d'un devis
function showQuotationDetails(button) {
    const quotationId = button.getAttribute('data-quotation-id');
    const quotationDate = button.getAttribute('data-quotation-date');
    const quotationSupplier = button.getAttribute('data-quotation-supplier');
    const quotationStatus = button.getAttribute('data-quotation-status');
    const quotationTotal = button.getAttribute('data-quotation-total');
    
    // Réinitialiser le formulaire
    document.getElementById('quotation-items').innerHTML = '';
    
    // Remplir le formulaire avec les données disponibles
    document.getElementById('quotation-name').value = quotationId;
    document.getElementById('quotation-date').value = quotationDate;
    document.getElementById('quotation-supplier').value = quotationSupplier;
    document.getElementById('quotation-status').value = quotationStatus;
    document.getElementById('quotation-total').value = quotationTotal;
    
    // Afficher le formulaire et masquer le spinner
    document.getElementById('loading-spinner').style.display = 'none';
    document.getElementById('quotationDetailsForm').style.display = 'block';
    
    // Afficher le modal personnalisé
    document.getElementById('customModal').style.display = 'block';
    
    // Optionnel : récupérer les articles du devis si nécessaire
    fetch(`/quotations/${quotationId}/items`)
    .then(response => {
        if (!response.ok) {
            throw new Error('Erreur lors de la récupération des articles');
        }
        return response.json();
    })
    .then(items => {
        const itemsContainer = document.getElementById('quotation-items');
        
        if (items && items.length > 0) {
            items.forEach(item => {
                itemsContainer.innerHTML += `
                    <tr style="border-bottom: 1px solid #444;">
                        <td style="padding: 10px;">${item.item_name || item.item_code}</td>
                        <td style="padding: 10px;">${item.qty} ${item.uom || ''}</td>
                        <td style="padding: 10px;">${new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(item.rate)}</td>
                        <td style="padding: 10px;">${new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(item.amount)}</td>
                    </tr>
                `;
            });
        } else {
            itemsContainer.innerHTML = '<tr><td colspan="4" style="padding: 10px; text-align: center;">Aucun article trouvé</td></tr>';
        }
    })
    .catch(error => {
        console.error('Erreur lors du chargement des articles:', error);
        document.getElementById('quotation-items').innerHTML = '<tr><td colspan="4" style="padding: 10px; text-align: center;">Erreur lors du chargement des articles</td></tr>';
    });
}


// Fonction pour fermer le modal
function closeModal() {
    document.getElementById('customModal').style.display = 'none';
}

// Fermer le modal si l'utilisateur clique en dehors
window.onclick = function(event) {
    const modal = document.getElementById('customModal');
    if (event.target === modal) {
        modal.style.display = 'none';
    }
}

// Ajouter une animation pour le spinner
document.addEventListener('DOMContentLoaded', function() {
    const style = document.createElement('style');
    style.innerHTML = `
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
    `;
    document.head.appendChild(style);
});