document.addEventListener('DOMContentLoaded', function() {
    const supplierFilter = document.getElementById('supplierFilter');
    
    if (supplierFilter) {
        supplierFilter.addEventListener('change', function() {
            const selectedSupplier = this.value;
            const rows = document.querySelectorAll('#purchase-orders-list tr');
            
            rows.forEach(row => {
                const supplierCell = row.cells[1];
                if (!selectedSupplier || supplierCell.textContent === selectedSupplier) {
                    row.style.display = '';
                } else {
                    row.style.display = 'none';
                }
            });
        });
    }
});

function viewPurchaseOrder(id) {
    window.location.href = `/purchase/details/${id}`;
}

function editPurchaseOrder(id) {
    window.location.href = `/purchase/edit/${id}`;
}