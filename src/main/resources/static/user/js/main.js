/**
 * Main JavaScript for Sneaker E-commerce
 */

document.addEventListener('DOMContentLoaded', function() {
    
    // 1. Quantity Selector Logic
    const qtyDecrements = document.querySelectorAll('.qty-btn-minus');
    const qtyIncrements = document.querySelectorAll('.qty-btn-plus');

    if (qtyDecrements && qtyIncrements) {
        qtyDecrements.forEach(btn => {
            btn.addEventListener('click', function() {
                const input = this.parentElement.querySelector('input');
                let value = parseInt(input.value);
                if (value > 1) {
                    input.value = value - 1;
                }
            });
        });

        qtyIncrements.forEach(btn => {
            btn.addEventListener('click', function() {
                const input = this.parentElement.querySelector('input');
                let value = parseInt(input.value);
                const max = parseInt(input.getAttribute('max')) || 10;
                if (value < max) {
                    input.value = value + 1;
                }
            });
        });
    }

    // 2. Product Gallery Thumbnail Click
    const thumbnails = document.querySelectorAll('.gallery-thumb');
    const mainImage = document.getElementById('mainProductImage');

    if (thumbnails.length > 0 && mainImage) {
        thumbnails.forEach(thumb => {
            thumb.addEventListener('click', function() {
                // Remove active class from all
                thumbnails.forEach(t => t.classList.remove('active'));
                // Add active class to clicked
                this.classList.add('active');
                
                // Change main image source
                const newSrc = this.querySelector('img').getAttribute('src');
                mainImage.setAttribute('src', newSrc);
            });
        });
    }

    // 3. Initialize Bootstrap Tooltips
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl)
    });

    // 4. Cart Add Notification Demo (Toast)
    const addToCartBtns = document.querySelectorAll('.btn-add-cart');
    addToCartBtns.forEach(btn => {
        btn.addEventListener('click', function(e) {
            if(!this.closest('form')) { // Only default prevent if not part of a form submission
                e.preventDefault();
                alert('Sản phẩm đã được thêm vào giỏ hàng!'); // Simple alert for demo
                // In a real app, use Bootstrap Toast or SweetAlert
            }
        });
    });

});
