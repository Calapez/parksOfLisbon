package pt.bruno.parksoflisbon

data class Park(val id: Int,
                val lat: Double,
                val lon: Double,
                val name: String,
                val address: String,
                val desc: String
)