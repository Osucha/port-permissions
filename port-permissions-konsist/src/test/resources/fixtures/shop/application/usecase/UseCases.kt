package shop.application.usecase

import com.osucha.portpermissions.AccessGuard
import com.osucha.portpermissions.PublicOperation

interface ListProducts {
    fun list(): List<String>
}

interface GuestCheckout {
    fun start(): String
}

interface CancelOrder {
    fun cancel(id: String)

    fun cancelAll()
}

class ListProductsService(
    private val guard: AccessGuard,
) : ListProducts {
    override fun list(): List<String> {
        guard.require(ShopPermissions.PRODUCTS_VIEW)
        return emptyList()
    }
}

class GuestCheckoutService : GuestCheckout {
    @PublicOperation("Guests check out before they have an account")
    override fun start(): String = "basket"
}

class CancelOrderService(
    private val guard: AccessGuard,
) : CancelOrder {
    override fun cancel(id: String) {
        guard.require(ShopPermissions.ORDERS_CANCEL)
    }

    override fun cancelAll() {
        require(true) { "stdlib require is not an authorization check" }
    }
}
